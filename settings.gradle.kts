rootProject.name = "scripted-quests"
include(":adapter_api")
include(":adapter_unsupported")
include(":adapter_v1_18_R2")
include(":scripted-quests")
project(":scripted-quests").projectDir = file("plugin")

pluginManagement {
  repositories {
    gradlePluginPortal()
    maven("https://papermc.io/repo/repository/maven-public/")
  }
}
