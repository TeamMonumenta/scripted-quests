package pe.scriptedquests.quests;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import pe.scriptedquests.Plugin;

class ActionRerunComponents implements ActionBase {
	private String mNpcName;
	private EntityType mEntityType;
	private boolean mLocked = false;

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
		if (!mLocked) {
			mLocked = true;
			plugin.mNpcManager.interactEvent(plugin, player, mNpcName, mEntityType);
			mLocked = false;
		} else {
			plugin.getLogger().severe("Stopped infinite loop for NPC '" + mNpcName + "'");
		}
	}
}
