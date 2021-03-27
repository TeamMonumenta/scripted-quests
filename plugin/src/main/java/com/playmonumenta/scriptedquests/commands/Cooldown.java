package com.playmonumenta.scriptedquests.commands;

import java.util.Collection;
import java.util.LinkedHashMap;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.ItemStackArgument;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class Cooldown {
	@SuppressWarnings("unchecked")
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("scriptedquests.cooldown");

		new CommandAPICommand("cooldown")
			.withPermission(perms)
			.withArguments(
				new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.MANY_PLAYERS),
				new ItemStackArgument("material"),
				new IntegerArgument("ticks")
			)
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>)args[0]) {
					player.setCooldown(((ItemStack)args[1]).getType(), (Integer)args[2]);
				}
			})
			.register();

	}
}
