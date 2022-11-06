import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import org.hidetake.groovy.ssh.core.Remote
import org.hidetake.groovy.ssh.core.RunHandler
import org.hidetake.groovy.ssh.core.Service
import org.hidetake.groovy.ssh.session.SessionHandler

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("com.playmonumenta.scriptedquests.java-conventions")
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1" // Generates plugin.yml
    id("org.hidetake.ssh") version "2.10.1"
    id("java")
    id("net.ltgt.errorprone") version "2.0.2"
    id("net.ltgt.nullaway") version "1.3.0"
}

dependencies {
    implementation(project(":adapter_api"))
    implementation(project(":adapter_unsupported"))
    implementation(project(":adapter_v1_16_R3"))
    implementation(project(":adapter_v1_18_R1", "reobf"))
    implementation(project(":adapter_v1_18_R2", "reobf"))
    implementation("com.google.api-client:google-api-client:1.31.4")
    implementation("com.google.apis:google-api-services-sheets:v4-rev1-1.21.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:0.1.0")
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("dev.jorel.CommandAPI:commandapi-core:6.0.0")
    compileOnly("com.mojang:brigadier:1.0.17")
    compileOnly("com.google.code.gson:gson:2.8.0")
    compileOnly("org.dynmap:DynmapCoreAPI:2.0")
    compileOnly("com.playmonumenta:redissync:1.7")
    compileOnly("com.comphenix.protocol:ProtocolLib:4.7.0")
    errorprone("com.google.errorprone:error_prone_core:2.10.0")
    errorprone("com.uber.nullaway:nullaway:0.9.5")
}

group = "com.playmonumenta"
description = "ScriptedQuests plugin"
version = rootProject.version

// Configure plugin.yml generation
bukkit {
    load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    main = "com.playmonumenta.scriptedquests.Plugin"
    apiVersion = "1.16"
    name = "ScriptedQuests"
    authors = listOf("The Monumenta Team")
    depend = listOf("CommandAPI")
    softDepend = listOf("dynmap", "MonumentaRedisSync", "ProtocolLib")
    commands {
        register("questtrigger") {
            description = "Invoked when a player clicks a chat message"
            permission = "scriptedquests.questtrigger"
            usage = "What are you doing? You shouldn't be using this!"
        }
        register("reloadquests") {
            description = "Reloads quest config files"
            permission = "scriptedquests.reloadquests"
            usage = "/reloadquests"
        }
        register("toggleclientchatapi") {
            description = "Toggles API"
            permission = "scriptedquests.toggleclientchatapi"
            usage = "/toggleclientchatapi"
        }
    }
}

// Relocation / shading
tasks {
    shadowJar {
        exclude("javax/**")
        exclude("com/sun/**")
        exclude("com/google/gdata/**")
        exclude("com/google/gson/**")
        exclude("com/google/errorprone/**")
        exclude("com/google/j2objc/**")
        exclude("com/google/thirdparty/**")
        exclude("org/apache/commons/**")
        exclude("org/apache/http/**")
        exclude("org/mortbay/**")
        relocate("com.fasterxml", "com.playmonumenta.scriptedquests.internal.com.fasterxml")
        relocate("com.google", "com.playmonumenta.scriptedquests.internal.com.google") {
            exclude("com/google/gson/**")
        }
        relocate("io.grpc", "com.playmonumenta.scriptedquests.internal.io.grpc")
        relocate("io.opencensus", "com.playmonumenta.scriptedquests.internal.io.opencensus")
        minimize {
            exclude(dependency("com.playmonumenta.*:.*:.*"))
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xmaxwarns")
    options.compilerArgs.add("10000")
    options.compilerArgs.add("-Xlint:deprecation")

    options.errorprone {
        // TODO This must be turned back on as soon as some of the other warnings are under control
        option("NullAway:AnnotatedPackages", "com.playmonumenta.DISABLE")

        allErrorsAsWarnings.set(true)

        /*** Disabled checks ***/
        // These we almost certainly don't want
        check("CatchAndPrintStackTrace", CheckSeverity.OFF) // This is the primary way a lot of exceptions are handled
        check("FutureReturnValueIgnored", CheckSeverity.OFF) // This one is dumb and doesn't let you check return values with .whenComplete()
        check("ImmutableEnumChecker", CheckSeverity.OFF) // Would like to turn this on but we'd have to annotate a bunch of base classes
        check("LockNotBeforeTry", CheckSeverity.OFF) // Very few locks in our code, those that we have are simple and refactoring like this would be ugly
        check("StaticAssignmentInConstructor", CheckSeverity.OFF) // We have tons of these on purpose
        check("StringSplitter", CheckSeverity.OFF) // We have a lot of string splits too which are fine for this use
        check("MutablePublicArray", CheckSeverity.OFF) // These are bad practice but annoying to refactor and low risk of actual bugs
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        project.shadow.component(this)
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/TeamMonumenta/scripted-quests")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

val basicssh = remotes.create("basicssh") {
    host = "admin-eu.playmonumenta.com"
    port = 8822
    user = "epic"
    knownHosts = allowAnyHosts
    agent = System.getenv("IDENTITY_FILE") == null
    identity = if (System.getenv("IDENTITY_FILE") == null) null else file(System.getenv("IDENTITY_FILE"))
}

val adminssh = remotes.create("adminssh") {
    host = "admin-eu.playmonumenta.com"
    port = 9922
    user = "epic"
    knownHosts = allowAnyHosts
    agent = System.getenv("IDENTITY_FILE") == null
    identity = if (System.getenv("IDENTITY_FILE") == null) null else file(System.getenv("IDENTITY_FILE"))
}

tasks.create("stage-deploy") {
    val shadowJar by tasks.named<ShadowJar>("shadowJar")
    dependsOn(shadowJar)
    doLast {
        ssh.runSessions {
            session(basicssh) {
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/stage/m12/server_config/plugins")
                execute("cd /home/epic/stage/m12/server_config/plugins && rm -f ScriptedQuests.jar && ln -s " + shadowJar.archiveFileName.get() + " ScriptedQuests.jar")
            }
        }
    }
}

tasks.create("build-deploy") {
    val shadowJar by tasks.named<ShadowJar>("shadowJar")
    dependsOn(shadowJar)
    doLast {
        ssh.runSessions {
            session(adminssh) {
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/project_epic/server_config/plugins")
                execute("cd /home/epic/project_epic/server_config/plugins && rm -f ScriptedQuests.jar && ln -s " + shadowJar.archiveFileName.get() + " ScriptedQuests.jar")
            }
        }
    }
}

tasks.create("play-deploy") {
    val shadowJar by tasks.named<ShadowJar>("shadowJar")
    dependsOn(shadowJar)
    doLast {
        ssh.runSessions {
            session(adminssh) {
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/play/m8/server_config/plugins")
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/play/m11/server_config/plugins")
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/play/m13/server_config/plugins")
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/play/m14/server_config/plugins")
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/play/m15/server_config/plugins")
                execute("cd /home/epic/play/m8/server_config/plugins && rm -f ScriptedQuests.jar && ln -s " + shadowJar.archiveFileName.get() + " ScriptedQuests.jar")
                execute("cd /home/epic/play/m11/server_config/plugins && rm -f ScriptedQuests.jar && ln -s " + shadowJar.archiveFileName.get() + " ScriptedQuests.jar")
                execute("cd /home/epic/play/m13/server_config/plugins && rm -f ScriptedQuests.jar && ln -s " + shadowJar.archiveFileName.get() + " ScriptedQuests.jar")
                execute("cd /home/epic/play/m14/server_config/plugins && rm -f ScriptedQuests.jar && ln -s " + shadowJar.archiveFileName.get() + " ScriptedQuests.jar")
                execute("cd /home/epic/play/m15/server_config/plugins && rm -f ScriptedQuests.jar && ln -s " + shadowJar.archiveFileName.get() + " ScriptedQuests.jar")
            }
        }
    }
}

fun Service.runSessions(action: RunHandler.() -> Unit) =
    run(delegateClosureOf(action))

fun RunHandler.session(vararg remotes: Remote, action: SessionHandler.() -> Unit) =
    session(*remotes, delegateClosureOf(action))

fun SessionHandler.put(from: Any, into: Any) =
    put(hashMapOf("from" to from, "into" to into))
