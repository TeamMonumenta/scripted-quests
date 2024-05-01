package com.playmonumenta.scriptedquests.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.ItemStackArgument;
import java.util.Collection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class Cooldown {
	@SuppressWarnings("unchecked")
	public static void register() {
		EntitySelectorArgument.ManyPlayers playerArg = new EntitySelectorArgument.ManyPlayers("player");
		Argument<ItemStack> materialArg = new ItemStackArgument("material");
		Argument<Integer> ticksArg = new IntegerArgument("ticks");

		new CommandAPICommand("cooldown")
			.withPermission(CommandPermission.fromString("scriptedquests.cooldown"))
			.withArguments(playerArg)
			.withArguments(materialArg)
			.withArguments(ticksArg)
			.executes((sender, args) -> {
				Collection<Player> players = args.getByArgument(playerArg);
				for (Player player : players) {
					player.setCooldown(args.getByArgument(materialArg).getType(), args.getByArgument(ticksArg));
				}
			})
			.register();
	}
}
