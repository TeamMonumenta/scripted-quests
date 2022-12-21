package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.scriptedquests.utils.LeaderboardUtils;
import com.playmonumenta.scriptedquests.utils.LeaderboardUtils.LeaderboardEntry;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.Nullable;

public class Leaderboard {
	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		new CommandAPICommand("leaderboard")
			.withPermission(CommandPermission.fromString("scriptedquests.leaderboard"))
			.withArguments(new EntitySelectorArgument.ManyPlayers("players"))
			.withArguments(new StringArgument("objective"))
			.withArguments(new BooleanArgument("descending"))
			.withArguments(new IntegerArgument("page", 1))
			.executes((sender, args) -> {
				Collection<Player> targets = (Collection<Player>) args[0];
				if (sender instanceof Player player) {
					if (!player.hasPermission("scriptedquests.leaderboard.others") && (targets.size() > 1 || !targets.contains(player))) {
						throw CommandAPI.failWithString("You do not have permission to run this as another player.");
					}
				}
				String objective = (String) args[1];
				Boolean descending = (Boolean) args[2];
				Integer pageNumber = (Integer) args[3];
				for (Player player : targets) {
					leaderboard(plugin, player, objective, descending, pageNumber, null);
				}
			})
			.register();

		new CommandAPICommand("leaderboard")
			.withPermission(CommandPermission.fromString("scriptedquests.leaderboard"))
			.withArguments(new EntitySelectorArgument.ManyPlayers("players"))
			.withArguments(new StringArgument("objective"))
			.withArguments(new BooleanArgument("descending"))
			.withArguments(new EntitySelectorArgument.ManyPlayers("filterPlayers"))
			.executes((sender, args) -> {
				Collection<Player> targets = (Collection<Player>) args[0];
				if (sender instanceof Player player) {
					if (!player.hasPermission("scriptedquests.leaderboard.others") && (targets.size() > 1 || !targets.contains(player))) {
						throw CommandAPI.failWithString("You do not have permission to run this as another player.");
					}
				}
				String objective = (String) args[1];
				Boolean descending = (Boolean) args[2];
				Collection<Player> filterPlayers = (Collection<Player>) args[3];
				for (Player player : targets) {
					leaderboard(plugin, player, objective, descending, 1, filterPlayers);
				}
			})
			.register();

		/*
		 * Add a command to copy a player's scoreboard to the Redis leaderboard if MonumentaRedisSync exists.
		 * If using scoreboards for leaderboards, this command does nothing
		 */
		new CommandAPICommand("leaderboard")
			.withPermission(CommandPermission.fromString("scriptedquests.leaderboard.update"))
			.withArguments(new MultiLiteralArgument("update"))
			.withArguments(new EntitySelectorArgument.ManyPlayers("players"))
			.withArguments(new StringArgument("objective"))
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>)args[1]) {
					leaderboardUpdate(player, (String)args[2]);
				}
			})
			.register();
	}

	public static void leaderboard(Plugin plugin, Player player, String objective, boolean descending, int page, @Nullable Collection<Player> filterPlayers) {
		List<LeaderboardEntry> entries = new ArrayList<>();

		/* Get the scoreboard objective (might be null) */
		final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		final Objective obj = scoreboard.getObjective(objective);

		/* If the scoreboard objective exists, use its display name */
		final Component displayName;
		if (obj != null) {
			 displayName = obj.displayName();
		} else {
			 displayName = Component.text(objective);
		}

		if (filterPlayers != null || !Bukkit.getServer().getPluginManager().isPluginEnabled("MonumentaRedisSync")) {
			/* Redis sync plugin not found - need to loop over scoreboards to compute leaderboard */

			if (obj == null) {
				player.sendMessage(ChatColor.RED + "The scoreboard objective '" + objective + "' does not exist");
				return;
			}
			if (filterPlayers == null) {
				/* Not filtering by players, get everyone on the scoreboard */
				for (String name : scoreboard.getEntries()) {
					Score score = obj.getScore(name);
					if (score.isScoreSet()) {
						int value = score.getScore();
						if (value != 0) {
							entries.add(new LeaderboardEntry(name, "", value));
						}
					}
				}
			} else {
				/* Filtering to specific players, only get their scores */
				for (Player filterPlayer : filterPlayers) {
					String name = filterPlayer.getName();
					Score score = obj.getScore(name);
					if (score.isScoreSet()) {
						int value = score.getScore();
						if (value != 0) {
							entries.add(new LeaderboardEntry(name, "", value));
						}
					}
				}

			}

			if (descending) {
				entries.sort(Collections.reverseOrder());
			} else {
				Collections.sort(entries);
			}

			colorizeEntries(entries, player.getName(), 0);

			LeaderboardUtils.sendLeaderboard(player, displayName, entries, page,
			                                 "/leaderboard " + player.getName() + " " + objective + (descending ? " true" : " false"),
											 filterPlayers == null /* Don't show pages for this variant */);
		} else {
			/* Redis sync plugin is available - use it instead */

			/* Get and process the data asynchronously using the redis database */
			Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
				try {
					/* TODO: Someday it'd be nice to just look up the appropriate range, and the player's value, rather than everything */
					Map<String, Integer> values = MonumentaRedisSyncAPI.getLeaderboard(objective, 0, -1, !descending).get();
					for (Map.Entry<String, Integer> entry : values.entrySet()) {
						entries.add(new LeaderboardEntry(entry.getKey(), "", entry.getValue()));
					}
					colorizeEntries(entries, player.getName(), 0);

					/* Send the leaderboard to the player back on the main thread */
					Bukkit.getScheduler().runTask(plugin, () -> LeaderboardUtils.sendLeaderboard(player, displayName, entries, page,
					                                 "/leaderboard " + player.getName() + " " + objective + (descending ? " true" : " false")));
				} catch (Exception ex) {
					plugin.getLogger().severe("Failed to generate leaderboard: " + ex.getMessage());
					ex.printStackTrace();
				}
			});
		}
	}

	public static void leaderboardUpdate(Player player, String objective) {
		if (Bukkit.getServer().getPluginManager().isPluginEnabled("MonumentaRedisSync")) {
			/* This command only does something if leaderboards are stored in Redis */

			/* Get the scoreboard objective */
			final Objective obj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objective);
			if (obj != null) {
				Score score = obj.getScore(player.getName());
				if (score.isScoreSet()) {
					score.getScore();
					MonumentaRedisSyncAPI.updateLeaderboardAsync(objective, player.getName(), score.getScore());
				}
			}
		}
	}

	private static void colorizeEntries(List<LeaderboardEntry> entries, String playerName, int index) {
		for (LeaderboardEntry entry : entries) {
			String color = switch (index) {
				case 0 -> "" + ChatColor.GOLD + ChatColor.BOLD;
				case 1 -> "" + ChatColor.WHITE + ChatColor.BOLD;
				case 2 -> "" + ChatColor.DARK_RED + ChatColor.BOLD;
				default -> "" + ChatColor.GRAY + ChatColor.BOLD;
			};
			if (entry.getName().equals(playerName)) {
				color = "" + ChatColor.BLUE + ChatColor.BOLD;
			}
			entry.setColor(color);
			index++;
		}
	}
}
