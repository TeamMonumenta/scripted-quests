package com.playmonumenta.scriptedquests.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.ItemStackArgument;
import java.util.Collection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class Cooldown {
	@SuppressWarnings("unchecked")
	public static void register() {
		new CommandAPICommand("cooldown")
			.withPermission(CommandPermission.fromString("scriptedquests.cooldown"))
			.withArguments(new EntitySelectorArgument.ManyPlayers("player"))
			.withArguments(new ItemStackArgument("material"))
			.withArguments(new IntegerArgument("ticks"))
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>)args[0]) {
					player.setCooldown(((ItemStack)args[1]).getType(), (Integer)args[2]);
				}
			})
			.register();
	}
}
