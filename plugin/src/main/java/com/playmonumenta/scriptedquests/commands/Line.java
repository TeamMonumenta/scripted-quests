package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.utils.BlockUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

public class Line {
	public static void register() {
		new CommandAPICommand("line")
			.withPermission(CommandPermission.fromString("scriptedquests.line"))
			.withArguments(new LocationArgument("start"))
			.withArguments(new LocationArgument("end"))
			.withArguments(new StringArgument("material"))
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
