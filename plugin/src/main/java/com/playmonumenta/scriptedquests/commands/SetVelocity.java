package com.playmonumenta.scriptedquests.commands;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.DoubleArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;

public class SetVelocity {
	@SuppressWarnings("unchecked")
	public static void register() {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("players", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		arguments.put("xvel", new DoubleArgument());
		arguments.put("yvel", new DoubleArgument());
		arguments.put("zvel", new DoubleArgument());

		CommandAPI.getInstance().register("setvelocity",
		                                  CommandPermission.fromString("scriptedquests.setvelocity"),
		                                  arguments,
		                                  (sender, args) -> {
											  for (Player player : (Collection<Player>)args[0]) {
												  player.setVelocity(new Vector((Double)args[1], (Double)args[2], (Double)args[3]));
											  }
		                                  }
		);
	}
}
