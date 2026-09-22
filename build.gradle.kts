import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
	id("com.playmonumenta.gradle-config") version "6.0.1-local"
}

monumenta {
	id("ScriptedQuests")
	name("ScriptedQuests")
	pmdWarningsAsErrors()
	checkstyleWarningsAsErrors()
	pluginProject(":scripted-quests")
	paper(
		"com.playmonumenta.scriptedquests.Plugin", BukkitPluginDescription.PluginLoadOrder.POSTWORLD, "26.1.2", "26.1.2.build.+",
		depends = listOf("CommandAPI", "MonumentaCommon"),
		softDepends = listOf("dynmap", "MonumentaRedisSync", "ProtocolLib"),
		action = {
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
