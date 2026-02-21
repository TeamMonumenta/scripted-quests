package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.utils.ExperienceUtils;
import org.bukkit.entity.Player;

public class PrerequisiteXPRaw implements PrerequisiteBase {

	private final int mXPNeeded;

	public PrerequisiteXPRaw(JsonElement value) throws Exception {
		try {
			mXPNeeded = Integer.parseInt(value.toString());
		} catch (NumberFormatException e) {
			throw new Exception("Error: " + value.toString() + " is not an integer!");
		}
	}

	@Override
	public boolean prerequisiteMet(QuestContext context) {
		if (context.getEntityUsedForPrerequisites() instanceof Player player) {
			return ExperienceUtils.getTotalExperience(player) >= mXPNeeded;
		}

		// Non-Player entities do not have XP
		return false;
	}
}
