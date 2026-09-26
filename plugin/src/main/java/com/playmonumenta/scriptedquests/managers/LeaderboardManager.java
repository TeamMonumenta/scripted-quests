package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.leaderboards.LeaderboardConfig;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import java.util.HashMap;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

public class LeaderboardManager {
	private final HashMap<String, LeaderboardConfig> mLeaderboards = new HashMap<>();

	public void reload(Plugin plugin, @Nullable CommandSender sender) {
		mLeaderboards.clear();

		QuestUtils.loadScriptedQuests(plugin,
			"leaderboards",
			sender,
			(object) -> {
				LeaderboardConfig cfg = new LeaderboardConfig(object);
				String objective = cfg.getObjective();

				if (mLeaderboards.containsKey(objective)) {
					throw new Exception(objective + " already exists");
				}

				mLeaderboards.put(objective, cfg);

				return objective;
			});
	}

	public @Nullable LeaderboardConfig get(String objective) {
		return mLeaderboards.get(objective);
	}

}
