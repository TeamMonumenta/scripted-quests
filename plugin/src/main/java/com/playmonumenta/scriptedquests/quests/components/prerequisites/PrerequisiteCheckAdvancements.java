package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.utils.InventoryUtils;

public class PrerequisiteCheckAdvancements implements PrerequisiteBase {
	private boolean mInverted;
	private Advancement mAdvancement;

	public PrerequisiteCheckAdvancements(JsonElement value) throws Exception {
		String advancement = value.getAsString();
		if (advancement == null) {
			throw new Exception("advancement value is not a string!");
		}

		if (advancement.charAt(0) == '!') {
			mInverted = true;
			advancement = advancement.substring(1);
		} else {
			mInverted = false;
		}

        mAdvancement = Bukkit.getAdvancement(InventoryUtils.getNamespacedKey(advancement));
        if (mAdvancement == null) {
			throw new Exception("Advancement '" + advancement + "' does not exist!");
        }
	}

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		if (entity instanceof Player) {
			return mInverted ^ ((Player)entity).getAdvancementProgress(mAdvancement).isDone();
		}

		// Non-player entities can't have advancements
		return false;
	}
}
