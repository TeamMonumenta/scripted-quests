package com.playmonumenta.scriptedquests.zones;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ZonePropertyChangeEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	private final Player mPlayer;
	private final String mNamespaceName;
	private final String mProperty;

	public ZonePropertyChangeEvent(Player player, String namespaceName, String property) {
		mPlayer = player;
		mNamespaceName = namespaceName;
		mProperty = property;
	}

	public Player getPlayer() {
		return mPlayer;
	}

	public String getNamespaceName() {
		return mNamespaceName;
	}

	public String getProperty() {
		return mProperty;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
