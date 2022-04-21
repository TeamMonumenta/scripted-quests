package com.playmonumenta.scriptedquests.zones;

import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ZoneChangeEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	private final String mLayer;
	private final Player mPlayer;
	private final @Nullable Zone mFromZone;
	private final @Nullable Zone mToZone;

	public ZoneChangeEvent(Player player, String layer, @Nullable Zone from, @Nullable Zone to) {
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
