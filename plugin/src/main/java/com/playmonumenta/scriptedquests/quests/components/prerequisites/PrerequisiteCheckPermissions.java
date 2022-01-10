package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import org.bukkit.entity.Entity;

import com.google.gson.JsonElement;

public class PrerequisiteCheckPermissions implements PrerequisiteBase {

	private final boolean mInverted;
	private final String mPermission;

	public PrerequisiteCheckPermissions(JsonElement value) throws Exception {
		String permission = value.getAsString();
		if (permission == null) {
			throw new Exception("permission value is not a string!");
		}

		if (permission.charAt(0) == '!') {
			mInverted = true;
			permission = permission.substring(1);
		} else {
			mInverted = false;
		}

		mPermission = permission;
	}

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		return mInverted ^ entity.hasPermission(mPermission);
	}

}
