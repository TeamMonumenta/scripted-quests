package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.scriptedquests.Plugin;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LocationArgument;

public class DebugZones {
	public static void register(Plugin plugin) {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("player", new EntitySelectorArgument(EntitySelectorArgument.EntitySelector.ONE_PLAYER));
		new CommandAPICommand("debugzones")
			.withPermission(CommandPermission.fromString("scriptedquests.debugzones"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				plugin.mZoneManager.sendDebug(sender, (Player) args[0]);
			})
			.register();

		arguments.clear();
		arguments.put("position", new LocationArgument());
		new CommandAPICommand("debugzones")
			.withPermission(CommandPermission.fromString("scriptedquests.debugzones"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				plugin.mZoneManager.sendDebug(sender, ((Location) args[0]).toVector());
			})
			.register();
	}
}
