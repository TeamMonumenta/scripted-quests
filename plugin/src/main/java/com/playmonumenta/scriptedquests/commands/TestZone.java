package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;

import org.bukkit.Location;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.TextArgument;

public class TestZone {
	public static void register(Plugin plugin) {
		new CommandAPICommand("testzones")
			.withPermission(CommandPermission.fromString("scriptedquests.testzones"))
			.withArguments(new LocationArgument("location"))
			.withArguments(new TextArgument("layer").replaceSuggestions(info -> {
				return plugin.mZoneManager.getLayerNameSuggestions();
			}))
			.executes((sender, args) -> {
				return inLayer(plugin, (Location) args[0], (String) args[1]);
			})
			.register();

		new CommandAPICommand("testzones")
			.withPermission(CommandPermission.fromString("scriptedquests.testzones"))
			.withArguments(new LocationArgument("location"))
			.withArguments(new TextArgument("layer").replaceSuggestions(info -> {
				return plugin.mZoneManager.getLayerNameSuggestions();
			}))
			.withArguments(new TextArgument("property").replaceSuggestions(info -> {
				return plugin.mZoneManager.getLoadedPropertySuggestions((String) info.previousArgs()[1]);
			}))
			.executes((sender, args) -> {
				return hasProperty(plugin, (Location) args[0], (String) args[1], (String) args[2]);
			})
			.register();
	}

	private static int inLayer(Plugin plugin, Location loc, String layer) {
		if (plugin.mZoneManager.getZone(loc, layer) == null) {
			return 0;
		}
		return 1;
	}

	private static int hasProperty(Plugin plugin, Location loc, String layer, String property) {
		boolean negated = property.startsWith("!");
		if (negated) {
			property = property.substring(1);
		}
		if (negated ^ plugin.mZoneManager.hasProperty(loc, layer, property)) {
			return 1;
		}
		return 0;
	}
}
