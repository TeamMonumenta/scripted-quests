package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.LocationArgument;
import io.github.jorelali.commandapi.api.arguments.StringArgument;

import com.playmonumenta.scriptedquests.utils.BlockUtils;

public class Line {
	@SuppressWarnings("unchecked")
	public static void register() {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("start", new LocationArgument());
		arguments.put("end", new LocationArgument());
		arguments.put("material", new StringArgument());

		CommandAPI.getInstance().register("line",
		                                  CommandPermission.fromString("scriptedquests.line"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      return run(sender, (Location) args[0], (Location) args[1], (String) args[2]);
		                                  }
		);
	}

	private static int run(CommandSender sender, Location start, Location end, String matStr) {
		Material mat = Material.getMaterial(matStr.toUpperCase());
		if (mat == null || !mat.isBlock()) {
			sender.sendMessage("Unknown block type " + matStr);
		}
		return BlockUtils.drawLine(start, end, mat);
	}
}
