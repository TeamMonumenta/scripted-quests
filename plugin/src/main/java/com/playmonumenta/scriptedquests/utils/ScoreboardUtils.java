package com.playmonumenta.scriptedquests.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

public class ScoreboardUtils {
	public static int getScoreboardValue(Entity entity, String scoreboardValue) {
		Objective objective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(scoreboardValue);
		if (objective != null) {
			if (entity instanceof Player) {
				return objective.getScore(entity.getName()).getScore();
			} else {
				return objective.getScore(entity.getUniqueId().toString()).getScore();
			}
		}

		return 0;
	}

	public static void setScoreboardValue(Entity entity, String scoreboardValue, int value) {
		Objective objective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(scoreboardValue);
		if (objective != null) {
			final Score score;
			if (entity instanceof Player) {
				score = objective.getScore(entity.getName());
			} else {
				score = objective.getScore(entity.getUniqueId().toString());
			}
			score.setScore(value);
		}
	}
}
