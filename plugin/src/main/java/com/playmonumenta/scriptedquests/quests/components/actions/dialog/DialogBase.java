package com.playmonumenta.scriptedquests.quests;

import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Plugin;

interface DialogBase {
	void sendDialog(Plugin plugin, Player player, QuestPrerequisites prereqs);
}
