import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml

plugins {
	id("com.playmonumenta.gradle-config") version "6.0.2-local"
}

monumenta {
	id("ScriptedQuests")
	name("ScriptedQuests")
	pmdWarningsAsErrors()
	checkstyleWarningsAsErrors()
	pluginProject(":scripted-quests")
	paper(
		"com.playmonumenta.scriptedquests.Plugin", BukkitPluginYaml.PluginLoadOrder.POSTWORLD, "26.1.2", "26.1.2.build.+",
		depends = listOf("CommandAPI", "MonumentaCommon"),
		softDepends = listOf("dynmap", "MonumentaRedisSync", "ProtocolLib"),
		bootstrapper = "com.playmonumenta.scriptedquests.ScriptedQuestsBootstrap",
	)

	versionAdapterApi("adapter_api", paper = "26.1.2.build.+") {
		dependencies {
			api("com.mojang:brigadier:1.0.17")
		}
	}

	versionAdapterUnsupported("adapter_unsupported")
	versionAdapter("adapter_v26_1_2", "26.1.2.build.+")
}

allprojects {
	tasks.withType<JavaCompile> {
		// TODO: revert before merge
		// options.compilerArgs.add("-Werror")
	}

	tasks.withType<Javadoc> {
		options {
			(this as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
		}
	}
}
