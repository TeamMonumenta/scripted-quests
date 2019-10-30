package com.playmonumenta.scriptedquests.managers;

import java.util.ArrayList;

import org.bukkit.command.CommandSender;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestDeath;
import com.playmonumenta.scriptedquests.utils.QuestUtils;

public class QuestDeathManager {
	private final ArrayList<QuestDeath> mDeaths = new ArrayList<QuestDeath>();

	/* If sender is non-null, it will be sent debugging information */
	public void reload(Plugin plugin, CommandSender sender) {
		mDeaths.clear();
		QuestUtils.loadScriptedQuests(plugin, "death", sender, (object) -> {
			mDeaths.add(new QuestDeath(object));
			return null;
		});
	}

	public boolean deathEvent(Plugin plugin, PlayerDeathEvent event) {
		/* Try each available death-triggered quest */
		for (QuestDeath death : mDeaths) {
			/* Stop after the first matching quest */
			if (death.deathEvent(plugin, event)) {
				return true;
			}
		}

		return false;
	}
}
