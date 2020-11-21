package com.playmonumenta.scriptedquests.commands;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.ItemStackArgument;


public class Cooldown {
	@SuppressWarnings("unchecked")
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("scriptedquests.cooldown");
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("player", new EntitySelectorArgument(EntitySelectorArgument.EntitySelector.MANY_PLAYERS));
		arguments.put("material", new ItemStackArgument());
		arguments.put("ticks", new IntegerArgument());

		new CommandAPICommand("cooldown")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>)args[0]) {
					player.setCooldown(((ItemStack)args[1]).getType(), (Integer)args[2]);
				}
			})
			.register();

	}
}
