package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import org.bukkit.entity.Entity;

public class PrerequisiteAlwaysTrue implements PrerequisiteBase {

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		return true;
	}

}
