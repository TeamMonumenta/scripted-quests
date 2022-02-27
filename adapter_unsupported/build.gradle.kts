plugins {
    id("com.playmonumenta.scriptedquests.java-conventions")
}

dependencies {
    implementation(project(":adapter_api"))
    compileOnly("com.destroystokyo.paper:paper-api:1.15.2-R0.1-SNAPSHOT")
    compileOnly("com.mojang:brigadier:1.0.17")
}

description = "adapter_unsupported"
version = rootProject.version
