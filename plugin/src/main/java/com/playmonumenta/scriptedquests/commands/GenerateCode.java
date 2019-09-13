package com.playmonumenta.scriptedquests.commands;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Plugin;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.TextArgument;

public class GenerateCode {
	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("scriptedquests.generatecode");
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("players", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		arguments.put("seed", new TextArgument());

		CommandAPI.getInstance().register("generatecode",
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
											  generateCode(plugin, (Collection<Player>)args[0], (String)args[1]);
		                                  }
		);
	}

	private static void generateCode(Plugin plugin, Collection<Player> players, String seed) {
		for (Player player : players) {
			plugin.mCodeManager.generateCodeForPlayer(player, seed);
		}
	}
}
