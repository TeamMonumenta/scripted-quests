package com.playmonumenta.scriptedquests.zones;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ZonePropertyChangeEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	private final Player mPlayer;
	private final String mLayer;
	private final String mProperty;

	public ZonePropertyChangeEvent(Player player, String layer, String property) {
		mPlayer = player;
		mLayer = layer;
		mProperty = property;
	}

	public Player getPlayer() {
		return mPlayer;
	}

	public String getLayer() {
		return mLayer;
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
