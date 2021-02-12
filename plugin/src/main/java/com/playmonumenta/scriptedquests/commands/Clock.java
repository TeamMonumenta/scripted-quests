package com.playmonumenta.scriptedquests.commands;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;

public class Clock {
	public static void register() {
		new CommandAPICommand("clock")
			.withPermission(CommandPermission.fromString("scriptedquests.clock"))
			.withArguments(new LocationArgument("location", LocationType.PRECISE_POSITION))
			.withArguments(new IntegerArgument("range"))
			.withArguments(new IntegerArgument("period"))
			.executes((sender, args) -> {
				if (sender instanceof Player) {
					Player player = (Player) sender;
					player.getWorld().spawn((Location) args[0], ArmorStand.class, (entity) -> {
						entity.addScoreboardTag("timer");
						entity.addScoreboardTag("range=" + (Integer)args[1]);
						entity.addScoreboardTag("period=" + (Integer)args[2]);
					});
				}
			})
			.register();
	}
}
