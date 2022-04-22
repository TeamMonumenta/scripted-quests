package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import java.util.Collection;
import org.bukkit.entity.Player;

public class GenerateCode {
	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		new CommandAPICommand("generatecode")
			.withPermission(CommandPermission.fromString("scriptedquests.generatecode"))
			.withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
			.withArguments(new TextArgument("seed"))
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
