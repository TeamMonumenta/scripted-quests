package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.scriptedquests.managers.GrowableManager;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.*;
import org.bukkit.Location;

public class Growable {
	public static void register(GrowableManager manager) {
		CommandPermission perm = CommandPermission.fromString("monumenta.growstructure");

		new CommandAPICommand("growable")
			.withPermission(perm)
			.withArguments(
				new LiteralArgument("grow"),
				new LocationArgument("location", LocationType.BLOCK_POSITION),
				new StringArgument("label").overrideSuggestions(manager.getLabels()),
				new IntegerArgument("ticksPerStep", 1),
				new IntegerArgument("blocksPerStep", 1),
				new BooleanArgument("callStructureGrowEvent")
			)
			.executes((sender, args) -> {
				try {
					int loaded = manager.grow((String)args[1], (Location)args[0], (Integer)args[2], (Integer)args[3], (Boolean)args[4]);
					sender.sendMessage("Successfully grew '" + (String)args[1] + "' placing " + Integer.toString(loaded) + " blocks");
				} catch (Exception e) {
					CommandAPI.fail(e.getMessage());
				}
			})
			.register();

		new CommandAPICommand("growable")
			.withPermission(perm)
			.withArguments(
				new LiteralArgument("add"),
				new LocationArgument("location", LocationType.BLOCK_POSITION),
				new StringArgument("label"),
				new IntegerArgument("maxDepth", 1)
			)
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
