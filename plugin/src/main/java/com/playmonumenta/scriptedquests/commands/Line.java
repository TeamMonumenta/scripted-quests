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
		LocationArgument startArg = new LocationArgument("start");
		LocationArgument endArg = new LocationArgument("end");
		StringArgument materialArg = new StringArgument("material");

		new CommandAPICommand("line")
			.withPermission(CommandPermission.fromString("scriptedquests.line"))
			.withArguments(startArg)
			.withArguments(endArg)
			.withArguments(materialArg)
			.executes((sender, args) -> {
				return run(sender, args.getByArgument(startArg), args.getByArgument(endArg), args.getByArgument(materialArg));
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
