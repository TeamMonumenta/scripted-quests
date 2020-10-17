package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import me.Novalescent.Core;
import me.Novalescent.player.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class PrerequisiteCheckClass implements PrerequisiteBase{

	private boolean mInverted;
	private String mClass;

	public PrerequisiteCheckClass(JsonElement value) throws Exception {
		String tag = value.getAsString();
		if (tag == null) {
			throw new Exception("class value is not a string!");
		}

		if (tag.charAt(0) == '!') {
			mInverted = true;
			mClass = tag.substring(1);
		} else {
			mInverted = false;
			mClass = tag;
		}
	}

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		if (entity instanceof Player) {
			Player player = (Player) entity;
			PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());

			if (data.mClassData != null) {
				return mInverted ^ data.mClassData.mClass.getId().equalsIgnoreCase(mClass.toLowerCase());
			}
		}
		return false;
	}
}
