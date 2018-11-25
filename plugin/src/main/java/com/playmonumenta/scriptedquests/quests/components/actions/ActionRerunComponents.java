package com.playmonumenta.scriptedquests.quests;

import com.playmonumenta.scriptedquests.Plugin;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

class ActionRerunComponents implements ActionBase {
	private final String mNpcName;
	private final EntityType mEntityType;

	// Array list should be faster than hashmap for such small set
	private final List<Player> mLocked = new ArrayList<Player>(10);

	ActionRerunComponents(String npcName, EntityType entityType) {
		mNpcName = npcName;
		mEntityType = entityType;
	}

	@Override
	public void doAction(Plugin plugin, Player player, QuestPrerequisites prereqs) {
		/*
		 * Prevent infinite loops by preventing this specific action
		 * from running itself again
		 */
		if (!mLocked.contains(player)) {
			mLocked.add(player);
			plugin.mNpcManager.interactEvent(plugin, player, mNpcName, mEntityType, true);
			mLocked.remove(player);
		} else {
			plugin.getLogger().severe("Stopped infinite loop for NPC '" + mNpcName + "'");
		}
	}
}
