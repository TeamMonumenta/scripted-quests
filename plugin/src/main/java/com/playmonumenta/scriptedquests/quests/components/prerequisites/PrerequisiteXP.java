package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.utils.ExperienceUtils;
import java.util.Map;
import java.util.Set;
import org.bukkit.entity.Player;

public class PrerequisiteXP implements PrerequisiteBase {

	private final boolean mLevelMode;
	private final int mXPNeeded;

	public PrerequisiteXP(JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("location value is not an object!");
		}

		Boolean levelMode = null;
		Integer xpNeeded = null;

		Set<Map.Entry<String, JsonElement>> entries = object.entrySet();
		for (Map.Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();
			JsonElement value = ent.getValue();

			if (key.equals("use_levels")) {
				levelMode = value.getAsBoolean();
			} else if (key.equals("xp_required")) {
				xpNeeded = value.getAsInt();
			} else {
				throw new Exception("Unknown xp key: '" + key + "'");
			}
		}

		if (levelMode == null || xpNeeded == null) {
			throw new Exception("XP prereq must have levelmode and levelsneeded");
		}

		mLevelMode = levelMode;
		mXPNeeded = xpNeeded;
	}

	@Override
	public boolean prerequisiteMet(QuestContext context) {
		if (context.getEntityUsedForPrerequisites() instanceof Player player) {
			if (mLevelMode) {
				return ExperienceUtils.getLevel(player) >= mXPNeeded;
			} else {
				return ExperienceUtils.getTotalExperience(player) >= mXPNeeded;
			}
		}

		// Non-Player entities do not have XP
		return false;
	}
}
