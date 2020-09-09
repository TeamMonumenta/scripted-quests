package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import com.playmonumenta.scriptedquests.utils.BlockUtils;

public class Line {
	@SuppressWarnings("unchecked")
	public static void register() {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("start", new LocationArgument());
		arguments.put("end", new LocationArgument());
		arguments.put("material", new StringArgument());

		new CommandAPICommand("line")
			.withPermission(CommandPermission.fromString("scriptedquests.line"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				return run(sender, (Location) args[0], (Location) args[1], (String) args[2]);
			})
			.register();

	}

	private static int run(CommandSender sender, Location start, Location end, String matStr) {
		Material mat = Material.getMaterial(matStr.toUpperCase());
		if (mat == null || !mat.isBlock()) {
			sender.sendMessage("Unknown block type " + matStr);
		}
		return BlockUtils.drawLine(start, end, mat);
	}
}
