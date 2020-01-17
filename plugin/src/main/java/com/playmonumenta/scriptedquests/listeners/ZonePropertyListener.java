package com.playmonumenta.scriptedquests.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.managers.ZonePropertyManager;
import com.playmonumenta.scriptedquests.zones.ZonePropertyChangeEvent;

public class ZonePropertyListener implements Listener {
	private Plugin mPlugin;

	public ZonePropertyListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void ZonePropertyChangeEvent(ZonePropertyChangeEvent event) {
		Player player = event.getPlayer();
		String layer = event.getLayer();
		String property = event.getProperty();

		mPlugin.mZonePropertyManager.changeEvent(mPlugin, player, layer, property);
	}
}
