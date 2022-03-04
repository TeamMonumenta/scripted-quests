package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import org.bukkit.entity.Player;

public class PrerequisiteSneaking implements PrerequisiteBase {
	private final boolean mValue;

	public PrerequisiteSneaking(JsonElement element) throws Exception {
		mValue = element.getAsBoolean();
	}

	@Override
	public boolean prerequisiteMet(QuestContext context) {
		if (context.getEntityUsedForPrerequisites() instanceof Player player) {
			return player.isSneaking() == mValue;
		}
		return false;
	}
}
