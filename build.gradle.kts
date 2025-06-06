import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
	id("com.playmonumenta.gradle-config") version "2.2+"
}

monumenta {
	name("ScriptedQuests")
	pluginProject(":scripted-quests")
	paper(
		"com.playmonumenta.scriptedquests.Plugin", BukkitPluginDescription.PluginLoadOrder.POSTWORLD, "1.18",
		depends = listOf("CommandAPI"),
		softDepends = listOf("dynmap", "MonumentaRedisSync", "ProtocolLib"),
		apiJarVersion = "1.20.4-R0.1-SNAPSHOT",
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

	versionAdapterApi("adapter_api", paper = "1.18.2") {
		dependencies {
			api("com.mojang:brigadier:1.0.17")
		}
	}

	versionAdapterUnsupported("adapter_unsupported")
	versionAdapter("adapter_v1_18_R2", "1.18.2")
	versionAdapter("adapter_v1_19_R3", "1.19.4")
	versionAdapter("adapter_v1_20_R3", "1.20.4")
}

allprojects {
	tasks.withType<Javadoc> {
		options {
			(this as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
		}
	}
}
