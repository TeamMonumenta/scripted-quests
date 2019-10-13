package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.google.gson.JsonElement;

public class PrerequisiteFullyHealed implements PrerequisiteBase {
	private final boolean mValue;

	public PrerequisiteFullyHealed(JsonElement element) throws Exception {
		mValue = element.getAsBoolean();
	}

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		if (entity instanceof LivingEntity) {
			LivingEntity le = (LivingEntity)entity;
			return !(mValue ^ (le.getHealth() > le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() - 0.5));
		}
		return mValue;
	}
}
