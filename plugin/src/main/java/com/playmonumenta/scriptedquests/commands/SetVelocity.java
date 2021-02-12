package com.playmonumenta.scriptedquests.commands;

import java.util.Collection;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;

public class SetVelocity {
	@SuppressWarnings("unchecked")
	public static void register() {
		new CommandAPICommand("setvelocity")
			.withPermission(CommandPermission.fromString("scriptedquests.setvelocity"))
			.withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
			.withArguments(new DoubleArgument("xvel"))
			.withArguments(new DoubleArgument("yvel"))
			.withArguments(new DoubleArgument("zvel"))
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>)args[0]) {
					player.setVelocity(new Vector((Double)args[1], (Double)args[2], (Double)args[3]));
				}
			})
			.register();
	}
}
