package com.playmonumenta.scriptedquests.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.managers.RaceManager;
import com.playmonumenta.scriptedquests.quests.QuestNpc;
import org.bukkit.Bukkit;
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
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityAddToWorldEvent(EntityAddToWorldEvent event) {
		Entity entity = event.getEntity();

		if (entity.getScoreboardTags().contains(RaceManager.ARMOR_STAND_RACE_TAG)) {
			Bukkit.getScheduler().runTask(mPlugin, () -> mPlugin.mRaceManager.removeIfNotActive(entity));
		}

		QuestNpc npc = mPlugin.mNpcManager.getInteractNPC(entity);
		if (npc != null) {
			// Invulnerable NPCs cannot be interacted with in some versions of Minecraft
			entity.setInvulnerable(false);
		}
	}
}
