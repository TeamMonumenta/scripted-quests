package pe.scriptedquests.quests;

import org.bukkit.entity.Player;

import pe.scriptedquests.Plugin;

interface ActionBase {
	public void doAction(Plugin plugin, Player player, QuestPrerequisites prereqs);
}
