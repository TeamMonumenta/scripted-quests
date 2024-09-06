repositories {
	maven("https://repo.mikeprimm.com")
}

dependencies {
	implementation(libs.bundles.google.api)

	compileOnly(libs.commandapi)
	compileOnly(libs.nbtapi)
	compileOnly(libs.brigadier)
	compileOnly(libs.gson)
	compileOnly(libs.dynmap)
	compileOnly(libs.protocollib)
	compileOnly(libs.redissync) {
		artifact {
			classifier = "all"
		}
	}

	testImplementation(libs.jupiter.api)
	testImplementation(libs.mockito)
	testImplementation(libs.mockbukkit)
	testRuntimeOnly(libs.jupiter.engine)
	testRuntimeOnly(libs.commandapi)
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
			exclude(project(":adapter_v1_18_R2"))
			exclude(project(":adapter_v1_19_R3"))
			exclude(project(":adapter_v1_20_R3"))
			exclude(dependency("com.playmonumenta.*:.*:.*"))
			exclude(dependency("dev.jorel.commandapi.*:.*:.*"))
		}
	}

	named<Test>("test") {
		useJUnitPlatform()
	}
}
