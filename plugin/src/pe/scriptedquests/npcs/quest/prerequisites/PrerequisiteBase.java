package pe.scriptedquests.npcs.quest.prerequisites;

import org.bukkit.entity.Player;

public interface PrerequisiteBase {
	public boolean prerequisiteMet(Player player);
}
