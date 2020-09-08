package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.utils.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

public class PrerequisiteCheckQuestData implements PrerequisiteBase {

	public PrerequisiteCheckQuestData(JsonElement value) throws Exception {
		String advancement = value.getAsString();
		if (advancement == null) {
			throw new Exception("quest data value is not a string!");
		}


	}

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		return false;
	}
}
