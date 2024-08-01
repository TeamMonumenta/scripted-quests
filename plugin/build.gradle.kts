import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
	id("com.github.johnrengelman.shadow") version "7.1.2"
	id("com.playmonumenta.scriptedquests.java-conventions")
	id("net.minecrell.plugin-yml.bukkit") version "0.5.1" // Generates plugin.yml
	id("java")
	id("net.ltgt.errorprone") version "3.1.0"
	id("net.ltgt.nullaway") version "1.6.0"
	id("com.playmonumenta.deployment") version "1.+"
}

repositories {
	maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
	implementation(project(":adapter_api"))
	implementation(project(":adapter_unsupported"))
	implementation(project(":adapter_v1_18_R2", "reobf"))
	implementation(project(":adapter_v1_19_R3", "reobf"))
	implementation("com.google.api-client:google-api-client:1.31.4")
	implementation("com.google.apis:google-api-services-sheets:v4-rev1-1.21.0")
	implementation("com.google.auth:google-auth-library-oauth2-http:0.1.0")
	compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
	compileOnly("dev.jorel:commandapi-bukkit-core:9.4.1")
	compileOnly("de.tr7zw:item-nbt-api-plugin:2.12.0-SNAPSHOT")
	compileOnly("com.mojang:brigadier:1.0.17")
	compileOnly("com.google.code.gson:gson:2.8.0")
	compileOnly("org.dynmap:DynmapCoreAPI:2.0")
	compileOnly("com.comphenix.protocol:ProtocolLib:4.7.0")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
	testImplementation("org.mockito:mockito-junit-jupiter:5.8.0")
	testImplementation("com.github.seeseemelk:MockBukkit-v1.20:3.9.0")
	testRuntimeOnly("dev.jorel:commandapi-bukkit-core:9.4.0")
	errorprone("com.google.errorprone:error_prone_core:2.29.1")
	errorprone("com.uber.nullaway:nullaway:0.9.5")
	compileOnly("com.playmonumenta:redissync:4.6.1:all")
}
configurations {
	testCompileOnly.get().extendsFrom(compileOnly.get())
}

group = "com.playmonumenta"
description = "ScriptedQuests plugin"
version = rootProject.version

// Configure plugin.yml generation
bukkit {
	load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
	main = "com.playmonumenta.scriptedquests.Plugin"
	apiVersion = "1.18"
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
			exclude(dependency("dev.jorel.commandapi.*:.*:.*"))
		}
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.compilerArgs.add("-Xmaxwarns")
	options.compilerArgs.add("10000")
	options.compilerArgs.add("-Xlint:deprecation")

	options.errorprone {
		option("NullAway:AnnotatedPackages", "com.playmonumenta.scriptedquests")

		allErrorsAsWarnings.set(true)

		/*** Disabled checks ***/
		// These we almost certainly don't want
		check(
			"InlineMeSuggester",
			CheckSeverity.OFF
		) // We won't keep deprecated stuff around long enough for this to matter
		check("CatchAndPrintStackTrace", CheckSeverity.OFF) // This is the primary way a lot of exceptions are handled
		check(
			"FutureReturnValueIgnored",
			CheckSeverity.OFF
		) // This one is dumb and doesn't let you check return values with .whenComplete()
		check(
			"ImmutableEnumChecker",
			CheckSeverity.OFF
		) // Would like to turn this on but we'd have to annotate a bunch of base classes
		check(
			"LockNotBeforeTry",
			CheckSeverity.OFF
		) // Very few locks in our code, those that we have are simple and refactoring like this would be ugly
		check("StaticAssignmentInConstructor", CheckSeverity.OFF) // We have tons of these on purpose
		check("StringSplitter", CheckSeverity.OFF) // We have a lot of string splits too which are fine for this use
		check(
			"MutablePublicArray",
			CheckSeverity.OFF
		) // These are bad practice but annoying to refactor and low risk of actual bugs
	}
}

tasks.named<Test>("test") {
	useJUnitPlatform()
}

java {
	withSourcesJar()
}

publishing {
	publications {
		create<MavenPublication>("maven") {
			artifact(tasks.shadowJar)
			artifact(tasks["sourcesJar"])
		}
	}
	repositories {
		maven {
			name = "MonumentaMaven"
			url = when (version.toString().endsWith("SNAPSHOT")) {
				true -> uri("https://maven.playmonumenta.com/snapshots")
				false -> uri("https://maven.playmonumenta.com/releases")
			}

			credentials {
				username = System.getenv("USERNAME")
				password = System.getenv("TOKEN")
			}
		}
	}
}

ssh.easySetup(tasks.named<ShadowJar>("shadowJar").get(), "ScriptedQuests")
