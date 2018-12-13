package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;

public interface DialogBase {
	void sendDialog(Plugin plugin, Player player, QuestPrerequisites prereqs);
}
