package com.playmonumenta.scriptedquests.listeners;

import com.google.gson.JsonObject;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import com.playmonumenta.redissync.event.PlayerServerTransferEvent;
import com.playmonumenta.redissync.event.PlayerTransferFailEvent;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.managers.RaceManager;
import com.playmonumenta.scriptedquests.zones.ZoneManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class RedisSyncListener implements Listener {
	private final Plugin mPlugin;

	public RedisSyncListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerServerTransferEvent(PlayerServerTransferEvent event) {
		Player player = event.getPlayer();
		mPlugin.mRaceManager.cancelRace(player);
		ZoneManager.getInstance().setTransferring(player, true);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerTransferFailEvent(PlayerTransferFailEvent event) {
		Player player = event.getPlayer();
		ZoneManager.getInstance().setTransferring(player, false);
	}

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

	@EventHandler(ignoreCancelled = true)
	public void onQuit(PlayerQuitEvent event) {
		Player p = event.getPlayer();
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			if (!p.isOnline()) {
				RaceManager.PLAYER_RACE_RING_PB_TIMES.remove(p.getUniqueId());
			}
		}, 100);
	}

}
