package com.playmonumenta.scriptedquests.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.managers.RaceManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

public class WorldListener implements Listener {
	private final Plugin mPlugin;

	public WorldListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityAddToWorldEvent(EntityAddToWorldEvent event) {
		Entity entity = event.getEntity();

		if (mPlugin.mNpcManager.isQuestNPC(entity)) {
			// Invulnerable NPCs cannot be interacted with in some versions of Minecraft
			entity.setInvulnerable(false);
		}

		// 1 tick delay due to issue removing entities from world
		new BukkitRunnable() {
			@Override
			public void run() {
				if (entity.isValid() && entity.getScoreboardTags().contains(RaceManager.ARMOR_STAND_RACE_TAG)) {
					Bukkit.getScheduler().runTask(mPlugin, () -> mPlugin.mRaceManager.removeIfNotActive(entity));
				}
			}
		}.runTaskLater(mPlugin, 1L);
	}
}
