package com.playmonumenta.scriptedquests.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.managers.QuestCompassManager;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.StringArgument;

public class Waypoint {
	public static void register(QuestCompassManager manager) {
		CommandPermission perm = CommandPermission.fromString("scriptedquests.waypoint");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
//		arguments.put("grow", new LiteralArgument("grow"));
		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));
		arguments.put("label", new StringArgument());
		arguments.put("location", new LocationArgument(LocationType.BLOCK_POSITION));

//		arguments.put("location", new LocationArgument(LocationType.BLOCK_POSITION));
//		arguments.put("label", new StringArgument().overrideSuggestions(manager.getLabels()));
//		arguments.put("ticksPerStep", new IntegerArgument(1));
//		arguments.put("blocksPerStep", new IntegerArgument(1));
//		arguments.put("callStructureGrowEvent", new BooleanArgument());

		new CommandAPICommand("waypoint")
			.withPermission(perm)
			.withArguments(arguments)
			.executes((sender, args) -> {
//				try {
					List<Location> waypoint = new ArrayList<>();
					waypoint.add((Location)args[2]);
					Bukkit.broadcastMessage(args[2] + " " + args[0] + " " + args[1]);
					manager.setCommandWaypoint((Player)args[0], waypoint, "Next Quest Location", (String)args[1]);
					Bukkit.broadcastMessage("success!");
//					int loaded = manager.grow((String)args[1], (Location)args[0], (Integer)args[2], (Integer)args[3], (Boolean)args[4]);
//					sender.sendMessage("Successfully grew '" + (String)args[1] + "' placing " + Integer.toString(loaded) + " blocks");
//				} catch (Exception e) {
//					CommandAPI.fail(e.getMessage());
//					e.printStackTrace();
//				}
			})
			.register();

//		arguments.clear();
//		arguments.put("add", new LiteralArgument("add"));
//		arguments.put("location", new LocationArgument(LocationType.BLOCK_POSITION));
//		arguments.put("label", new StringArgument());
//		arguments.put("maxDepth", new IntegerArgument(1));
//
//		new CommandAPICommand("growable")
//			.withPermission(perm)
//			.withArguments(arguments)
//			.executes((sender, args) -> {
//				try {
//					int size = manager.add((String)args[1], (Location)args[0], (Integer)args[2]);
//					sender.sendMessage("Successfully saved '" + (String)args[1] + "' containing " + Integer.toString(size) + " blocks");
//				} catch (Exception e) {
//					CommandAPI.fail(e.getMessage());
//				}
//			})
//			.register();
	}
}
