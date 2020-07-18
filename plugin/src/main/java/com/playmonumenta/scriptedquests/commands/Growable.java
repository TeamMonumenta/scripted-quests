package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.scriptedquests.managers.GrowableManager;

import org.bukkit.Location;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.BooleanArgument;
import io.github.jorelali.commandapi.api.arguments.DynamicSuggestedStringArgument;
import io.github.jorelali.commandapi.api.arguments.IntegerArgument;
import io.github.jorelali.commandapi.api.arguments.LiteralArgument;
import io.github.jorelali.commandapi.api.arguments.LocationArgument;
import io.github.jorelali.commandapi.api.arguments.StringArgument;
import io.github.jorelali.commandapi.api.arguments.LocationArgument.LocationType;

public class Growable {
	public static void register(GrowableManager manager) {
		CommandPermission perm = CommandPermission.fromString("monumenta.growstructure");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("grow", new LiteralArgument("grow"));
		arguments.put("location", new LocationArgument(LocationType.BLOCK_POSITION));
		arguments.put("label", new DynamicSuggestedStringArgument(() -> manager.getLabels()));
		arguments.put("ticksPerStep", new IntegerArgument(1));
		arguments.put("blocksPerStep", new IntegerArgument(1));
		arguments.put("callStructureGrowEvent", new BooleanArgument());

		CommandAPI.getInstance().register("growable",
		                                  perm,
		                                  arguments,
		                                  (sender, args) -> {
											  try {
												  int loaded = manager.grow((String)args[1], (Location)args[0], (Integer)args[2], (Integer)args[3], (Boolean)args[4]);
												  sender.sendMessage("Successfully grew '" + (String)args[1] + "' placing " + Integer.toString(loaded) + " blocks");
											  } catch (Exception e) {
												  CommandAPI.fail(e.getMessage());
											  }
		                                  }
		);

		arguments.clear();
		arguments.put("add", new LiteralArgument("add"));
		arguments.put("location", new LocationArgument(LocationType.BLOCK_POSITION));
		arguments.put("label", new StringArgument());
		arguments.put("maxDepth", new IntegerArgument(1));

		CommandAPI.getInstance().register("growable",
		                                  perm,
		                                  arguments,
		                                  (sender, args) -> {
											  try {
												  int size = manager.add((String)args[1], (Location)args[0], (Integer)args[2]);
												  sender.sendMessage("Successfully saved '" + (String)args[1] + "' containing " + Integer.toString(size) + " blocks");
											  } catch (Exception e) {
												  CommandAPI.fail(e.getMessage());
											  }
		                                  }
		);
	}
}
