repositories {
	maven("https://repo.mikeprimm.com")
}

dependencies {
	implementation("com.google.api-client:google-api-client:1.31.4")
	implementation("com.google.apis:google-api-services-sheets:v4-rev1-1.21.0")
	implementation("com.google.auth:google-auth-library-oauth2-http:0.1.0")
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
	compileOnly("com.playmonumenta:redissync:4.6.1:all")
}

configurations {
	testCompileOnly.get().extendsFrom(compileOnly.get())
}

description = "ScriptedQuests plugin"

// Relocation / shading4
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

	named<Test>("test") {
		useJUnitPlatform()
	}
}
