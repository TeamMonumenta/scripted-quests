package com.playmonumenta.scriptedquests.quests;

import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Plugin;

interface ActionBase {
	void doAction(Plugin plugin, Player player, QuestPrerequisites prereqs);
}
