package com.playmonumenta.scriptedquests.commands;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Plugin;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.IntegerArgument;
import io.github.jorelali.commandapi.api.arguments.LiteralArgument;
import io.github.jorelali.commandapi.api.arguments.StringArgument;

public class Race {
	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		/* First one of these has both required arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("start", new LiteralArgument("start"));
		arguments.put("players", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		arguments.put("raceLabel", new StringArgument());

		CommandAPI.getInstance().register("race",
		                                  CommandPermission.NONE,
		                                  arguments,
		                                  (sender, args) -> {
		                                      raceStart(plugin, sender, (Collection<Player>)args[0],
		                                                (String)args[1]);
		                                  }
		);

		arguments = new LinkedHashMap<>();
		arguments.put("stop", new LiteralArgument("stop"));
		arguments.put("players", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		CommandAPI.getInstance().register("race",
		                                  CommandPermission.NONE,
		                                  arguments,
		                                  (sender, args) -> {
		                                      raceStop(plugin, sender, (Collection<Player>)args[0]);
		                                  }
		);

		arguments = new LinkedHashMap<>();
		arguments.put("leaderboard", new LiteralArgument("leaderboard"));
		arguments.put("players", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		arguments.put("raceLabel", new StringArgument());
		arguments.put("page", new IntegerArgument(1)); // Min 1
		CommandAPI.getInstance().register("race",
		                                  CommandPermission.NONE,
		                                  arguments,
		                                  (sender, args) -> {
		                                      raceLeaderboard(plugin, sender, (Collection<Player>)args[0],
		                                                      (String)args[1], (Integer)args[2]);
		                                  }
		);
	}

	private static void raceStart(Plugin plugin, CommandSender sender,
	                              Collection<Player>players, String raceLabel) {
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
	                             Collection<Player>players) {
		// Check permission
		if (sender.hasPermission("scriptedquests.race")) {
			if (plugin.mRaceManager != null) {
				for (Player player : players) {
					plugin.mRaceManager.cancelRace(player);
				}
			}
		}
	}

	private static void raceLeaderboard(Plugin plugin, CommandSender sender,
	                                    Collection<Player>players, String raceLabel, int page) {
		// Anyone can use this - no permission check
		if (plugin.mRaceManager != null) {
			for (Player player : players) {
				plugin.mRaceManager.sendLeaderboard(player, raceLabel, page);
			}
		}
	}
}
