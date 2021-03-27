package com.playmonumenta.scriptedquests.commands;

import java.util.Collection;
import java.util.LinkedHashMap;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SetVelocity {
	@SuppressWarnings("unchecked")
	public static void register() {
		new CommandAPICommand("setvelocity")
			.withPermission(CommandPermission.fromString("scriptedquests.setvelocity"))
			.withSubcommand(new CommandAPICommand("directional")
				.withArguments(
					new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS),
					new DoubleArgument("magnitude"),
					new BooleanArgument("inverted")
				)
				.executes((sender, args) -> {
						for (Player player : (Collection<Player>)args[0]) {
							Vector dir = player.getLocation().getDirection();
							if ((Boolean) args[1]) {
								dir = dir.multiply(-1);
							}
							player.setVelocity(dir.multiply((Double) args[2]));
						}
					})
			)
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
