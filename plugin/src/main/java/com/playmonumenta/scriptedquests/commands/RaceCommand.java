package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.Collection;
import org.bukkit.entity.Player;

public class RaceCommand {
	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		new CommandAPICommand("race")
			.withSubcommand(new CommandAPICommand("start")
				.withPermission(CommandPermission.fromString("scriptedquests.race.start"))
				.withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
				.withArguments(new StringArgument("raceLabel"))
				.executes((sender, args) -> {
					raceStart(plugin, (Collection<Player>) args[0],
						(String) args[1]);
				}))
			.withSubcommand(new CommandAPICommand("stop")
				.withPermission(CommandPermission.fromString("scriptedquests.race.stop"))
				.withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
				.executes((sender, args) -> {
					raceStop(plugin, (Collection<Player>)args[0]);
				}))
			.withSubcommand(new CommandAPICommand("win")
				.withPermission(CommandPermission.fromString("scriptedquests.race.win"))
				.withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
				.executes((sender, args) -> {
					raceWin(plugin, (Collection<Player>)args[0]);
				}))
			.withSubcommand(new CommandAPICommand("leaderboard")
				.withPermission(CommandPermission.fromString("scriptedquests.race.leaderboard"))
				.withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
				.withArguments(new StringArgument("raceLabel"))
				.withArguments(new IntegerArgument("page", 1))
				.executes((sender, args) -> {
					raceLeaderboard(plugin, (Collection<Player>)args[0],
						(String)args[1], (Integer)args[2]);
				}))
			.register();
	}

	private static void raceStart(Plugin plugin, Collection<Player> players, String raceLabel) {
		if (plugin.mRaceManager != null) {
			for (Player player : players) {
				plugin.mRaceManager.startRace(player, raceLabel);
			}
		}
	}

	private static void raceStop(Plugin plugin, Collection<Player> players) {
		if (plugin.mRaceManager != null) {
			for (Player player : players) {
				plugin.mRaceManager.cancelRace(player);
			}
		}
	}

	private static void raceWin(Plugin plugin, Collection<Player> players) {
		if (plugin.mRaceManager != null) {
			for (Player player : players) {
				plugin.mRaceManager.winRace(player);
			}
		}
	}

	private static void raceLeaderboard(Plugin plugin, Collection<Player> players, String raceLabel, int page) {
		if (plugin.mRaceManager != null) {
			for (Player player : players) {
				plugin.mRaceManager.sendLeaderboard(player, raceLabel, page);
			}
		}
	}
}
