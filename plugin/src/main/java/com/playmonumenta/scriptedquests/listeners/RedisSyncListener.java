package com.playmonumenta.scriptedquests.listeners;

import com.google.gson.JsonObject;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import com.playmonumenta.redissync.event.PlayerSessionEndEvent;
import com.playmonumenta.scriptedquests.managers.RaceManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class RedisSyncListener implements Listener {
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerSaveEvent(PlayerSaveEvent event) {
		Player player = event.getPlayer();
		JsonObject playerRingPBData = RaceManager.PLAYER_RACE_RING_PB_TIMES.get(player.getUniqueId());
		if (playerRingPBData != null) {
			event.setPluginData(RaceManager.REDIS_RACE_RING_PBS_PATH, playerRingPBData);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		JsonObject playerRingPBData = MonumentaRedisSyncAPI.getPlayerPluginData(player.getUniqueId(), RaceManager.REDIS_RACE_RING_PBS_PATH);
		if (playerRingPBData != null) {
			RaceManager.PLAYER_RACE_RING_PB_TIMES.put(player.getUniqueId(), playerRingPBData);
		}
	}

	/* After the final save, which saves the times */
	@EventHandler(ignoreCancelled = true)
	public void playerSessionEndEvent(PlayerSessionEndEvent event) {
		RaceManager.PLAYER_RACE_RING_PB_TIMES.remove(event.getPlayer().getUniqueId());
	}

}
