package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class PrerequisiteGamemode implements PrerequisiteBase {
	private final ArrayList<GameMode> mValues = new ArrayList<>();

	public PrerequisiteGamemode(JsonElement element) throws Exception {
		if (!element.isJsonArray()) {
			throw new Exception("gamemode value is not an array");
		}
		for (JsonElement value : element.getAsJsonArray()) {
			if (value.getAsString() == null) {
				throw new Exception("gamemode array value is not a string");
			}
			switch (value.getAsString()) {
				case "survival":
					mValues.add(GameMode.SURVIVAL);
					break;
				case "creative":
					mValues.add(GameMode.CREATIVE);
					break;
				case "adventure":
					mValues.add(GameMode.ADVENTURE);
					break;
				case "spectator":
					mValues.add(GameMode.SPECTATOR);
					break;
				default:
					throw new Exception("Invalid gamemode: "+value.getAsString());
			}
		}
	}

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		if (entity instanceof Player) {
			Player player = (Player) entity;
			return mValues.contains(player.getGameMode());
		}
		return false;
	}
}
