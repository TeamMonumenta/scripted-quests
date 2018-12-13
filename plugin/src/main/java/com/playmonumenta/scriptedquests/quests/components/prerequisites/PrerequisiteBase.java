package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import org.bukkit.entity.Player;

public interface PrerequisiteBase {
	boolean prerequisiteMet(Player player);
}
