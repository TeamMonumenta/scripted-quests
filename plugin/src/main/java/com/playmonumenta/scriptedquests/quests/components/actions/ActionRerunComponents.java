package com.playmonumenta.scriptedquests.quests.components.actions;

import com.playmonumenta.scriptedquests.quests.QuestContext;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class ActionRerunComponents implements ActionBase {
	private final String mNpcName;
	private final EntityType mEntityType;

	// Array list should be faster than hashmap for such small set
	private final List<Player> mLocked = new ArrayList<Player>(10);

	public ActionRerunComponents(String npcName, EntityType entityType) {
		mNpcName = npcName;
		mEntityType = entityType;
	}

	@Override
	public void doActions(QuestContext context) {
		/*
		 * Prevent infinite loops by preventing this specific action
		 * from running itself again
		 */
		if (!mLocked.contains(context.getPlayer())) {
			mLocked.add(context.getPlayer());
			try {
				context.getPlugin().mNpcManager.interactEvent(context.clearPrerequisites().useNpcForPrerequisites(false), mNpcName, mEntityType, true);
			} finally {
				mLocked.remove(context.getPlayer());
			}
		} else {
			context.getPlugin().getLogger().severe("Stopped infinite loop for NPC '" + mNpcName + "'");
		}
	}
}
