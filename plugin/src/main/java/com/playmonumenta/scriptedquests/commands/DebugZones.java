package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelector;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Plugin;

public class DebugZones {
	public static void register(Plugin plugin) {

		new CommandAPICommand("debugzones")
			.withPermission(CommandPermission.fromString("scriptedquests.debugzones"))
			.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
			.executes((sender, args) -> {
				plugin.mZoneManager.sendDebug(sender, (Player) args[0]);
			})
			.register();

		new CommandAPICommand("debugzones")
			.withPermission(CommandPermission.fromString("scriptedquests.debugzones"))
			.withArguments(new LocationArgument("position"))
			.executes((sender, args) -> {
				plugin.mZoneManager.sendDebug(sender, ((Location) args[0]).toVector());
			})
			.register();

	}
}
