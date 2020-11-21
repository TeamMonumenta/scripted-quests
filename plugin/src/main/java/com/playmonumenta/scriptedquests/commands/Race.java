package com.playmonumenta.scriptedquests.commands;

import java.util.Collection;
import java.util.LinkedHashMap;

import com.playmonumenta.scriptedquests.Plugin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public class Race {
	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		/* First one of these has both required arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("start", new LiteralArgument("start"));
		arguments.put("players", new EntitySelectorArgument(EntitySelectorArgument.EntitySelector.MANY_PLAYERS));
		arguments.put("raceLabel", new StringArgument());
		new CommandAPICommand("race")
			.withPermission(CommandPermission.NONE)
			.withArguments(arguments)
			.executes((sender, args) -> {
				raceStart(plugin, sender, (Collection<Player>)args[0],
					(String)args[1]);
			})
			.register();

		arguments.clear();
		arguments.put("stop", new LiteralArgument("stop"));
		arguments.put("players", new EntitySelectorArgument(EntitySelectorArgument.EntitySelector.MANY_PLAYERS));
		new CommandAPICommand("race")
			.withPermission(CommandPermission.NONE)
			.withArguments(arguments)
			.executes((sender, args) -> {
				raceStop(plugin, sender, (Collection<Player>)args[0]);
			})
			.register();

		arguments.clear();
		arguments.put("win", new LiteralArgument("win"));
		arguments.put("players", new EntitySelectorArgument(EntitySelectorArgument.EntitySelector.MANY_PLAYERS));
		new CommandAPICommand("race")
			.withPermission(CommandPermission.NONE)
			.withArguments(arguments)
			.executes((sender, args) -> {
				raceWin(plugin, sender, (Collection<Player>)args[0]);
			})
			.register();

		arguments.clear();
		arguments.put("leaderboard", new LiteralArgument("leaderboard"));
		arguments.put("players", new EntitySelectorArgument(EntitySelectorArgument.EntitySelector.MANY_PLAYERS));
		arguments.put("raceLabel", new StringArgument());
		arguments.put("page", new IntegerArgument(1)); // Min 1
		new CommandAPICommand("race")
			.withPermission(CommandPermission.NONE)
			.withArguments(arguments)
			.executes((sender, args) -> {
				raceLeaderboard(plugin, (Collection<Player>)args[0],
					(String)args[1], (Integer)args[2]);
			})
			.register();
	}

	private static void raceStart(Plugin plugin, CommandSender sender,
	                              Collection<Player> players, String raceLabel) {
		// Check permission
		if (sender.hasPermission("scriptedquests.race")) {
			if (plugin.mRaceManager != null) {
				for (Player player : players) {
					plugin.mRaceManager.startRace(player, raceLabel);
				}
			}
		}
	}

	private static void raceStop(Plugin plugin, CommandSender sender,
	                             Collection<Player> players) {
		// Check permission
		if (sender.hasPermission("scriptedquests.race")) {
			if (plugin.mRaceManager != null) {
				for (Player player : players) {
					plugin.mRaceManager.cancelRace(player);
				}
			}
		}
	}

	private static void raceWin(Plugin plugin, CommandSender sender,
								Collection<Player> players) {
		// Check permission
		if (sender.hasPermission("scriptedquests.race")) {
			if (plugin.mRaceManager != null) {
				for (Player player : players) {
					plugin.mRaceManager.winRace(player);
				}
			}
		}
	}

	private static void raceLeaderboard(Plugin plugin, Collection<Player> players, String raceLabel, int page) {
		// Anyone can use this - no permission check
		if (plugin.mRaceManager != null) {
			for (Player player : players) {
				plugin.mRaceManager.sendLeaderboard(player, raceLabel, page);
			}
		}
	}
}
