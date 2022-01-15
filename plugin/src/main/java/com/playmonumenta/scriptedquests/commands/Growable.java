package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.managers.GrowableManager;

import org.bukkit.Location;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.StringArgument;

public class Growable {
	public static void register(GrowableManager manager) {
		CommandPermission perm = CommandPermission.fromString("monumenta.growstructure");

		new CommandAPICommand("growable")
			.withPermission(perm)
			.withSubcommand(new CommandAPICommand("grow")
				.withArguments(new LocationArgument("location", LocationType.BLOCK_POSITION))
				.withArguments(new StringArgument("label").replaceSuggestions(info -> {
					return manager.getLabels();
				}))
				.withArguments(new IntegerArgument("ticksPerStep", 1))
				.withArguments(new IntegerArgument("blocksPerStep", 1))
				.withArguments(new BooleanArgument("callStructureGrowEvent"))
				.executes((sender, args) -> {
					try {
						int loaded = manager.grow((String)args[1], (Location)args[0], (Integer)args[2], (Integer)args[3], (Boolean)args[4]);
						sender.sendMessage("Successfully grew '" + (String)args[1] + "' placing " + Integer.toString(loaded) + " blocks");
					} catch (Exception e) {
						CommandAPI.fail(e.getMessage());
					}
				}))
			.withSubcommand(new CommandAPICommand("add")
				.withArguments(new LocationArgument("location", LocationType.BLOCK_POSITION))
				.withArguments(new StringArgument("label"))
				.withArguments(new IntegerArgument("maxDepth", 1))
				.executes((sender, args) -> {
					try {
						int size = manager.add((String)args[1], (Location)args[0], (Integer)args[2]);
						sender.sendMessage("Successfully saved '" + (String)args[1] + "' containing " + Integer.toString(size) + " blocks");
					} catch (Exception e) {
						CommandAPI.fail(e.getMessage());
					}
				}))
			.register();
	}
}
