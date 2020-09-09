package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

public class Clock {
	@SuppressWarnings("unchecked")
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("scriptedquests.clock");
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("location", new LocationArgument(LocationType.PRECISE_POSITION));
		arguments.put("range", new IntegerArgument());
		arguments.put("period", new IntegerArgument());

		new CommandAPICommand("clock")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
					if (sender instanceof Player) {
						Player player = (Player) sender;
						player.getWorld().spawn((Location) args[0], ArmorStand.class, (entity) -> {
							entity.addScoreboardTag("timer");
							entity.addScoreboardTag("range=" + args[1]);
							entity.addScoreboardTag("period=" + args[2]);
						});
					}
				})
			.register();;

	}
}
