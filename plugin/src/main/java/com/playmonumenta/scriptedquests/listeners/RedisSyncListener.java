package com.playmonumenta.scriptedquests.listeners;

import com.playmonumenta.redissync.event.PlayerServerTransferEvent;
import com.playmonumenta.redissync.event.PlayerTransferFailEvent;
import com.playmonumenta.scriptedquests.Plugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class RedisSyncListener implements Listener {
	private final Plugin mPlugin;

	public RedisSyncListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerServerTransferEvent(PlayerServerTransferEvent event) {
		Player player = event.getPlayer();
		mPlugin.mRaceManager.cancelRace(player);
		mPlugin.mZoneManager.setTransferring(player, true);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerTransferFailEvent(PlayerTransferFailEvent event) {
		Player player = event.getPlayer();
		mPlugin.mZoneManager.setTransferring(player, false);
	}
}
