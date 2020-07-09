package com.playmonumenta.scriptedquests.commands;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.IntegerArgument;
import io.github.jorelali.commandapi.api.arguments.ItemStackArgument;

public class Cooldown {
	@SuppressWarnings("unchecked")
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("scriptedquests.cooldown");
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("player", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		arguments.put("material", new ItemStackArgument());
		arguments.put("ticks", new IntegerArgument());

		CommandAPI.getInstance().register("cooldown", perms, arguments, (sender, args) -> {
			for (Player player : (Collection<Player>)args[0]) {
				player.setCooldown(((ItemStack)args[1]).getType(), (Integer)args[2]);
			}
		});
	}
}
