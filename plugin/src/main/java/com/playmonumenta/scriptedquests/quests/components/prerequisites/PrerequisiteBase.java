package com.playmonumenta.scriptedquests.quests;

import org.bukkit.entity.Player;

interface PrerequisiteBase {
	boolean prerequisiteMet(Player player);
}
