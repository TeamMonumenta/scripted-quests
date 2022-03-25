package com.playmonumenta.scriptedquests.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestNpc;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class WorldListener implements Listener {
	private final Plugin mPlugin;

	public WorldListener(Plugin plugin) {
		mPlugin = plugin;
	}

	//  A Chunk Loaded.
	@EventHandler(priority = EventPriority.LOWEST)
	public void entityAddToWorldEvent(EntityAddToWorldEvent event) {
		Entity entity = event.getEntity();

		mPlugin.mRaceManager.removeIfNotActive(entity);

		QuestNpc npc = mPlugin.mNpcManager.getInteractNPC(entity.getName(), entity.getType());
		if (npc != null) {
			// Invulnerable NPCs cannot be interacted with in some versions of Minecraft
			entity.setInvulnerable(false);
		}
	}
}
