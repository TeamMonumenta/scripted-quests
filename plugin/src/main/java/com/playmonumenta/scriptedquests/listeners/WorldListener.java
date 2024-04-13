package com.playmonumenta.scriptedquests.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.managers.RaceManager;
import com.playmonumenta.scriptedquests.quests.QuestNpc;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.Nullable;

public class WorldListener implements Listener {
	private final Plugin mPlugin;

	public WorldListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityAddToWorldEvent(EntityAddToWorldEvent event) {
		Entity entity = event.getEntity();

		if (entity.getScoreboardTags().contains(RaceManager.ARMOR_STAND_RACE_TAG)) {
			Bukkit.getScheduler().runTask(mPlugin, () -> mPlugin.mRaceManager.removeIfNotActive(entity));
		}

		if (mPlugin.mNpcManager.isQuestNPC(entity)) {
			// Invulnerable NPCs cannot be interacted with in some versions of Minecraft
			entity.setInvulnerable(false);

			List<QuestNpc> npcs = mPlugin.mNpcManager.getInteractNPC(entity);
			if (npcs != null) {
				@Nullable String skin = null;
				for (QuestNpc file : npcs) {
					skin = file.getPlayerSkin() != null ? file.getPlayerSkin() : skin;
				}
				if (skin != null) {
					entity.getPersistentDataContainer().set(QuestNpc.PLAYER_SKIN_KEY, org.bukkit.persistence.PersistentDataType.STRING, skin);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void worldLoadEvent(WorldLoadEvent event) {
		mPlugin.mZoneManager.onLoadWorld(event.getWorld());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void worldUnloadEvent(WorldUnloadEvent event) {
		mPlugin.mZoneManager.onUnloadWorld(event.getWorld());
	}
}
