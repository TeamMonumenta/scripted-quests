package pe.scriptedquests.quests;

import org.bukkit.entity.Player;

interface PrerequisiteBase {
	public boolean prerequisiteMet(Player player);
}
