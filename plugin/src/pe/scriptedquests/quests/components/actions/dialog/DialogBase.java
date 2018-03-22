package pe.scriptedquests.quests;

import org.bukkit.entity.Player;

import pe.scriptedquests.Plugin;

interface DialogBase {
	public void sendDialog(Plugin plugin, Player player, QuestPrerequisites prereqs);
}
