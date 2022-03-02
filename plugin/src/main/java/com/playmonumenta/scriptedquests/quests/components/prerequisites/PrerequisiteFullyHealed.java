package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class PrerequisiteFullyHealed implements PrerequisiteBase {
	private final boolean mValue;

	public PrerequisiteFullyHealed(JsonElement element) throws Exception {
		mValue = element.getAsBoolean();
	}

	@Override
	public boolean prerequisiteMet(QuestContext context) {
		if (context.getEntityUsedForPrerequisites() instanceof LivingEntity le) {
			return mValue == (le.getHealth() > le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() - 0.5);
		}
		return mValue;
	}
}
