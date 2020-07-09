package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.IntegerArgument;
import io.github.jorelali.commandapi.api.arguments.LocationArgument;
import io.github.jorelali.commandapi.api.arguments.LocationArgument.LocationType;

public class Clock {
	@SuppressWarnings("unchecked")
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("scriptedquests.clock");
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("location", new LocationArgument(LocationType.PRECISE_POSITION));
		arguments.put("range", new IntegerArgument());
		arguments.put("period", new IntegerArgument());

		CommandAPI.getInstance().register("clock", perms, arguments, (sender, args) -> {
			if (sender instanceof Player) {
				Player player = (Player)sender;
				player.getWorld().spawn((Location)args[0], ArmorStand.class, (entity) -> {
					entity.addScoreboardTag("timer");
					entity.addScoreboardTag("range=" + args[1]);
					entity.addScoreboardTag("period=" + args[2]);
				});
			}
		});
	}
}
