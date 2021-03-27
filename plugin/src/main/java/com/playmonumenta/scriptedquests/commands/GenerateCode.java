package com.playmonumenta.scriptedquests.commands;

import java.util.Collection;
import java.util.LinkedHashMap;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Plugin;

public class GenerateCode {
	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("scriptedquests.generatecode");

		new CommandAPICommand("generatecode")
			.withPermission(perms)
			.withArguments(
				new EntitySelectorArgument("players",EntitySelectorArgument.EntitySelector.MANY_PLAYERS),
				new TextArgument("seed")
			)
			.executes((sender, args) -> {
				generateCode(plugin, (Collection<Player>)args[0], (String)args[1]);
			})
			.register();

	}

	private static void generateCode(Plugin plugin, Collection<Player> players, String seed) {
		for (Player player : players) {
			plugin.mCodeManager.generateCodeForPlayer(player, seed);
		}
	}
}
