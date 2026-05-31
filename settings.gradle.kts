rootProject.name = "scripted-quests"
include(":adapter_api")
include(":adapter_unsupported")
include(":adapter_v1_20_R3")
include(":adapter_26_1_2")
include(":scripted-quests")
project(":scripted-quests").projectDir = file("plugin")

pluginManagement {
	repositories {
		gradlePluginPortal()
		maven("https://maven.playmonumenta.com/releases")
	}
}
