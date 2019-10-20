package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.google.gson.JsonElement;

public class PrerequisiteSneaking implements PrerequisiteBase {
	private final boolean mValue;

	public PrerequisiteSneaking(JsonElement element) throws Exception {
		mValue = element.getAsBoolean();
	}

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		if (entity instanceof Player) {
			Player player = (Player) entity;
			return player.isSneaking() == mValue;
		}
		return false;
	}
}
