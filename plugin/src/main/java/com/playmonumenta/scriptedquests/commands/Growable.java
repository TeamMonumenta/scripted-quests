package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.scriptedquests.managers.GrowableManager;

import org.bukkit.Location;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.StringArgument;

public class Growable {
	public static void register(GrowableManager manager) {
		CommandPermission perm = CommandPermission.fromString("monumenta.growstructure");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("grow", new LiteralArgument("grow"));
		arguments.put("location", new LocationArgument(LocationType.BLOCK_POSITION));
		arguments.put("label", new StringArgument().overrideSuggestions(manager.getLabels()));
		arguments.put("ticksPerStep", new IntegerArgument(1));
		arguments.put("blocksPerStep", new IntegerArgument(1));
		arguments.put("callStructureGrowEvent", new BooleanArgument());

		new CommandAPICommand("growable")
			.withPermission(perm)
			.withArguments(arguments)
			.executes((sender, args) -> {
				try {
					int loaded = manager.grow((String)args[1], (Location)args[0], (Integer)args[2], (Integer)args[3], (Boolean)args[4]);
					sender.sendMessage("Successfully grew '" + (String)args[1] + "' placing " + Integer.toString(loaded) + " blocks");
				} catch (Exception e) {
					CommandAPI.fail(e.getMessage());
				}
			})
			.register();

		arguments.clear();
		arguments.put("add", new LiteralArgument("add"));
		arguments.put("location", new LocationArgument(LocationType.BLOCK_POSITION));
		arguments.put("label", new StringArgument());
		arguments.put("maxDepth", new IntegerArgument(1));

		new CommandAPICommand("growable")
			.withPermission(perm)
			.withArguments(arguments)
			.executes((sender, args) -> {
				try {
					int size = manager.add((String)args[1], (Location)args[0], (Integer)args[2]);
					sender.sendMessage("Successfully saved '" + (String)args[1] + "' containing " + Integer.toString(size) + " blocks");
				} catch (Exception e) {
					CommandAPI.fail(e.getMessage());
				}
			})
			.register();
	}
}
