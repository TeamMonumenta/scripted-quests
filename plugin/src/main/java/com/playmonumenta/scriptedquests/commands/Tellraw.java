package com.playmonumenta.scriptedquests.commands;

import java.util.Collection;
import java.util.SplittableRandom;

import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.managers.TranslationsManager;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.AdventureChatComponentArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;

import net.kyori.adventure.text.Component;

public class Tellraw {
	public static void register() {
		CommandAPI.unregister("tellraw");

		new CommandAPICommand("tellraw")
			.withPermission(CommandPermission.OP)
			.withArguments(new EntitySelectorArgument("targets", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
			.withArguments(new AdventureChatComponentArgument("message"))
			.executes((sender, args) -> {
				Collection<Player> targets = (Collection<Player>)args[0];
				Component message = (Component)args[1];
				for (Player player : targets) {
					player.sendMessage(TranslationsManager.translate(player, message));
				}
				return targets.size();
			})
			.register();
	}
}
