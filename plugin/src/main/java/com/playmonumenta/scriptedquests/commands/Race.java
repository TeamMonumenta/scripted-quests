package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.managers.RaceManager;

import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.IntegerArgument;
import io.github.jorelali.commandapi.api.arguments.LiteralArgument;
import io.github.jorelali.commandapi.api.arguments.StringArgument;
import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Race {
	@SuppressWarnings("unchecked")
	public static void register(RaceManager manager) {
		/* First one of these has both required arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("start", new LiteralArgument("start"));
		arguments.put("players", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		arguments.put("raceLabel", new StringArgument());

		CommandAPI.getInstance().register("race",
		                                  new CommandPermission("scriptedquests.race.start"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      raceStart(manager, sender, (Collection<Player>)args[0],
		                                                (String)args[1]);
		                                  }
		);

		arguments = new LinkedHashMap<>();
		arguments.put("stop", new LiteralArgument("stop"));
		arguments.put("players", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		CommandAPI.getInstance().register("race",
		                                  new CommandPermission("scriptedquests.race.stop"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      raceStop(manager, sender, (Collection<Player>)args[0]);
		                                  }
		);

		arguments = new LinkedHashMap<>();
		arguments.put("leaderboard", new LiteralArgument("leaderboard"));
		arguments.put("players", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		arguments.put("raceLabel", new StringArgument());
		arguments.put("startLine", new IntegerArgument(1)); // Min 1
		CommandAPI.getInstance().register("race",
		                                  new CommandPermission("scriptedquests.race.leaderboard"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      raceLeaderboard(manager, sender, (Collection<Player>)args[0],
		                                                      (String)args[1], (Integer)args[2]);
		                                  }
		);
	}

	private static void raceStart(RaceManager manager, CommandSender sender,
	                              Collection<Player>players, String raceLabel) {
		for (Player player : players) {
			manager.startRace(player, raceLabel);
		}
	}

	private static void raceStop(RaceManager manager, CommandSender sender,
	                             Collection<Player>players) {
		for (Player player : players) {
			manager.cancelRace(player);
		}
	}

	private static void raceLeaderboard(RaceManager manager, CommandSender sender,
	                                    Collection<Player>players, String raceLabel, int startLine) {
		for (Player player : players) {
			//TODO
		}
	}
}
