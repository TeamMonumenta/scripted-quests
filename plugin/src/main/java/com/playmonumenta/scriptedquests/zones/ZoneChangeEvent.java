package com.playmonumenta.scriptedquests.zones;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ZoneChangeEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	private final String mLayer;
	private final Player mPlayer;
	private final Zone mFromZone;
	private final Zone mToZone;

	public ZoneChangeEvent(Player player, String layer, Zone from, Zone to) {
		mPlayer = player;
		mLayer = layer;
		mFromZone = from;
		mToZone = to;
	}

	public Player getPlayer() {
		return mPlayer;
	}

	public String getLayer() {
		return mLayer;
	}

	public @Nullable Zone getFromZone() {
		return mFromZone;
	}

	public @Nullable Zone getToZone() {
		return mToZone;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
