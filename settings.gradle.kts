rootProject.name = "scripted-quests"
include(":adapter_api")
include(":adapter_unsupported")
include(":adapter_v26_1_2")
include(":scripted-quests")
project(":scripted-quests").projectDir = file("plugin")

pluginManagement {
	repositories {
		mavenLocal()
		gradlePluginPortal()
		maven("https://maven.playmonumenta.com/releases")
	}
}
