package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import dev.jorel.commandapi.CommandAPI;
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
		EntitySelectorArgument.ManyPlayers playersArg = new EntitySelectorArgument.ManyPlayers("players");
		StringArgument labelArg = new StringArgument("raceLabel");
		IntegerArgument pageArg = new IntegerArgument("page", 1);

		new CommandAPICommand("race")
			.withSubcommand(new CommandAPICommand("start")
				.withPermission(CommandPermission.fromString("scriptedquests.race.start"))
				.withArguments(playersArg)
				.withArguments(labelArg)
				.executes((sender, args) -> {
					raceStart(plugin, args.getByArgument(playersArg), args.getByArgument(labelArg));
				}))
			.withSubcommand(new CommandAPICommand("stop")
				.withPermission(CommandPermission.fromString("scriptedquests.race.stop"))
				.withArguments(playersArg)
				.executes((sender, args) -> {
					raceStop(plugin, args.getByArgument(playersArg));
				}))
			.withSubcommand(new CommandAPICommand("win")
				.withPermission(CommandPermission.fromString("scriptedquests.race.win"))
				.withArguments(playersArg)
				.executes((sender, args) -> {
					raceWin(plugin, args.getByArgument(playersArg));
				}))
			.withSubcommand(new CommandAPICommand("leaderboard")
				.withPermission(CommandPermission.fromString("scriptedquests.race.leaderboard"))
				.withArguments(playersArg)
				.withArguments(labelArg)
				.withArguments(pageArg)
				.executes((sender, args) -> {
					Collection<Player> targets = args.getByArgument(playersArg);
					if (sender instanceof Player player) {
						if (!player.hasPermission("scriptedquests.race.leaderboard.others") && (targets.size() > 1 || !targets.contains(player))) {
							throw CommandAPI.failWithString("You do not have permission to run this as another player.");
						}
					}
					String raceLabel = args.getByArgument(labelArg);
					int pageNumber = args.getByArgument(pageArg);
					raceLeaderboard(plugin, targets, raceLabel, pageNumber);
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
