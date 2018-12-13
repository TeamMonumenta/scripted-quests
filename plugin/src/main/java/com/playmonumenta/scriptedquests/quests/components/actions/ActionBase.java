package com.playmonumenta.scriptedquests.quests.components.actions;

import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;

public interface ActionBase {
	void doAction(Plugin plugin, Player player, QuestPrerequisites prereqs);
}
