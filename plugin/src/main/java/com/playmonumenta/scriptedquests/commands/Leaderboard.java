package com.playmonumenta.scriptedquests.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.scriptedquests.utils.LeaderboardUtils;
import com.playmonumenta.scriptedquests.utils.LeaderboardUtils.LeaderboardEntry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public class Leaderboard {
	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("players", new EntitySelectorArgument(EntitySelectorArgument.EntitySelector.MANY_PLAYERS));
		arguments.put("objective", new StringArgument());
		arguments.put("descending", new BooleanArgument());
		arguments.put("page", new IntegerArgument(1)); // Min 1
		new CommandAPICommand("leaderboard")
			.withPermission(CommandPermission.fromString("scriptedquests.leaderboard"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>)args[0]) {
					leaderboard(plugin, player, (String)args[1],
						(Boolean)args[2], (Integer)args[3]);
				}
			})
			.register();

		/*
		 * Add a command to copy a player's scoreboard to the Redis leaderboard if MonumentaRedisSync exists.
		 * If using scoreboards for leaderboards, this command does nothing
		 */
		arguments.clear();
		arguments.put("update", new LiteralArgument("update"));
		arguments.put("players", new EntitySelectorArgument(EntitySelectorArgument.EntitySelector.MANY_PLAYERS));
		arguments.put("objective", new StringArgument());
		new CommandAPICommand("leaderboard")
			.withPermission(CommandPermission.fromString("scriptedquests.leaderboard"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>)args[0]) {
					leaderboardUpdate(player, (String)args[1]);
				}
			})
			.register();
	}

	public static void leaderboard(Plugin plugin, Player player, String objective, boolean descending, int page) {
		List<LeaderboardEntry> entries = new ArrayList<LeaderboardEntry>();

		/* Get the scoreboard objective (might be null) */
		final Objective obj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objective);

		/* If the scoreboard objective exists, use its display name */
		final String displayName;
		if (obj != null) {
			 displayName = obj.getDisplayName();
		} else {
			 displayName = objective;
		}

		if (Bukkit.getServer().getPluginManager().getPlugin("MonumentaRedisSync") == null) {
			/* Redis sync plugin not found - need to loop over scoreboards to compute leaderboard */

			if (obj == null) {
				player.sendMessage(ChatColor.RED + "The scoreboard objective '" + objective + "' does not exist");
				return;
			}
			for (String name : obj.getScoreboard().getEntries()) {
				Score score = obj.getScore(name);
				if (score.isScoreSet()) {
					int value = score.getScore();
					if (value != 0) {
						entries.add(new LeaderboardEntry(name, "", value));
					}
				}
			}

			if (descending) {
				Collections.sort(entries, Collections.reverseOrder());
			} else {
				Collections.sort(entries);
			}

			colorizeEntries(entries, player.getName(), 0);

			LeaderboardUtils.sendLeaderboard(player, displayName, entries, page,
			                                 "/leaderboard " + player.getName() + " " + objective + (descending ? " true" : " false"));
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
					Bukkit.getScheduler().runTask(plugin, () -> {
						LeaderboardUtils.sendLeaderboard(player, displayName, entries, page,
						                                 "/leaderboard " + player.getName() + " " + objective + (descending ? " true" : " false"));

					});
				} catch (Exception ex) {
					plugin.getLogger().severe("Failed to generate leaderboard: " + ex.getMessage());
					ex.printStackTrace();
				}
			});
		}
	}

	public static void leaderboardUpdate(Player player, String objective) {
		if (Bukkit.getServer().getPluginManager().getPlugin("MonumentaRedisSync") != null) {
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
			String color;
			switch (index) {
			case (0):
				color = "" + ChatColor.GOLD + ChatColor.BOLD;
				break;
			case (1):
				color = "" + ChatColor.WHITE + ChatColor.BOLD;
				break;
			case (2):
				color = "" + ChatColor.DARK_RED + ChatColor.BOLD;
				break;
			default:
				color = "" + ChatColor.GRAY + ChatColor.BOLD;
			}
			if (entry.getName().equals(playerName)) {
				color = "" + ChatColor.BLUE + ChatColor.BOLD;
			}
			entry.setColor(color);
			index++;
		}
	}
}
