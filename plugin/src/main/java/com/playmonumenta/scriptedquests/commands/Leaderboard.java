package com.playmonumenta.scriptedquests.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

import com.playmonumenta.scriptedquests.utils.LeaderboardUtils;
import com.playmonumenta.scriptedquests.utils.LeaderboardUtils.LeaderboardEntry;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.BooleanArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.IntegerArgument;
import io.github.jorelali.commandapi.api.arguments.StringArgument;

public class Leaderboard {
	@SuppressWarnings("unchecked")
	public static void register() {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("players", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		arguments.put("objective", new StringArgument());
		arguments.put("descending", new BooleanArgument());
		arguments.put("page", new IntegerArgument(1)); // Min 1

		CommandAPI.getInstance().register("leaderboard",
		                                  CommandPermission.fromString("scriptedquests.leaderboard"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      for (Player player : (Collection<Player>)args[0]) {
		                                          leaderboard(player, (String)args[1],
		                                                      (Boolean)args[2], (Integer)args[3]);
		                                      }
		                                  }
		);
	}

	public static void leaderboard(Player player, String objective, boolean descending, int page) {
		List<LeaderboardEntry> entries = new ArrayList<LeaderboardEntry>();

		Objective obj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objective);
		if (obj == null) {
			player.sendMessage(ChatColor.RED + "The scoreboard objective '" + objective + "' does not exist");
			return;
		}
		String displayName = obj.getDisplayName();
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

		int index = 0;
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
			if (entry.getName().equals(player.getName())) {
				color = "" + ChatColor.BLUE + ChatColor.BOLD;
			}
			entry.setColor(color);
			index++;
		}

		LeaderboardUtils.sendLeaderboard(player, displayName, entries, page,
		                                 "/leaderboard @s " + objective + (descending ? " true" : " false"));
	}
}
