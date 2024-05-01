package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import java.util.Collection;
import org.bukkit.entity.Player;

public class GenerateCode {
	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		EntitySelectorArgument.ManyPlayers playersArg = new EntitySelectorArgument.ManyPlayers("players");
		Argument<String> seedArg = new TextArgument("seed");

		new CommandAPICommand("generatecode")
			.withPermission(CommandPermission.fromString("scriptedquests.generatecode"))
			.withArguments(playersArg)
			.withArguments(seedArg)
			.executes((sender, args) -> {
				generateCode(plugin, args.getByArgument(playersArg), args.getByArgument(seedArg));
			})
			.register();
	}

	private static void generateCode(Plugin plugin, Collection<Player> players, String seed) {
		for (Player player : players) {
			plugin.mCodeManager.generateCodeForPlayer(player, seed);
		}
	}
}
