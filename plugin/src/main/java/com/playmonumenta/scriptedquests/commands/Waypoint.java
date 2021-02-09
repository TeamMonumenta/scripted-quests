package com.playmonumenta.scriptedquests.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Plugin;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.TextArgument;
import net.md_5.bungee.api.ChatColor;

public class Waypoint {
	public static void register(Plugin plugin) {
		CommandPermission perm = CommandPermission.fromString("scriptedquests.waypoint");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		//Sets command waypoint
		arguments.clear();
		arguments.put("set", new LiteralArgument("set"));
		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));
		arguments.put("title", new TextArgument());
		arguments.put("label",  new TextArgument());
		arguments.put("location", new LocationArgument(LocationType.PRECISE_POSITION));
		new CommandAPICommand("waypoint")
			.withPermission(perm)
			.withArguments(arguments)
			.executes((sender, args) -> {
				if (plugin.mQuestCompassManager != null) {
					List<Location> waypoint = new ArrayList<>();
					waypoint.add((Location)args[3]);
					plugin.mQuestCompassManager.setCommandWaypoint((Player)args[0], waypoint, (String)args[1] + ChatColor.RESET, (String)args[2]);
				} else {
					CommandAPI.fail("Quest Compass Manager does not exist!");
				}
			})
			.register();

		//Remove the waypoint from a player
		arguments.clear();
		arguments.put("remove", new LiteralArgument("remove"));
		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));

		new CommandAPICommand("waypoint")
			.withPermission(perm)
			.withArguments(arguments)
			.executes((sender, args) -> {
				if (plugin.mQuestCompassManager != null) {
					plugin.mQuestCompassManager.removeCommandWaypoint((Player)args[0]);
				} else {
					CommandAPI.fail("Quest Compass Manager does not exist!");
				}
			})
			.register();
	}
}
