package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.zones.Zone;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.TextArgument;

public class TestZone {
	public static void register(Plugin plugin) {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("location", new LocationArgument());
		arguments.put("layer", new TextArgument());
		new CommandAPICommand("testzones")
			.withPermission(CommandPermission.fromString("scriptedquests.testzones"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				return inLayer(plugin, (Location) args[0], (String) args[1]);
			})
			.register();
		arguments.clear();

		arguments.put("location", new LocationArgument());
		arguments.put("layer", new TextArgument());
		arguments.put("property", new TextArgument());
		new CommandAPICommand("testzones")
			.withPermission(CommandPermission.fromString("scriptedquests.testzones"))
			.withArguments(arguments)
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
		if (plugin.mZoneManager.hasProperty(loc, layer, property)) {
			return 1;
		}
		return 0;
	}
}
