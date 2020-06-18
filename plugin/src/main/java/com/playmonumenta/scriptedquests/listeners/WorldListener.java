package com.playmonumenta.scriptedquests.listeners;

import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestNpc;

public class WorldListener implements Listener {
	private final Plugin mPlugin;

	public WorldListener(Plugin plugin) {
		mPlugin = plugin;
	}

	//  A Chunk Loaded.
	@EventHandler(priority = EventPriority.LOWEST)
	public void chunkLoadEvent(ChunkLoadEvent event) {
		Chunk chunk = event.getChunk();
		for (Entity entity : chunk.getEntities()) {
			QuestNpc npc = mPlugin.mNpcManager.getInteractNPC(entity.getName(), entity.getType());
			if (npc != null) {
				// Invulnerable NPCs cannot be interacted with in some versions of Minecraft
				entity.setInvulnerable(false);
			}
		}
	}
}
