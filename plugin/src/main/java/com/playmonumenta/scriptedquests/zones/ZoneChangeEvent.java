package com.playmonumenta.scriptedquests.zones;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.playmonumenta.scriptedquests.zones.zone.Zone;

public class ZoneChangeEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	private final Player mPlayer;
	private final Zone mFromZone;
	private final Zone mToZone;

	public ZoneChangeEvent(Player player, Zone from, Zone to) {
		mPlayer = player;
		mFromZone = from;
		mToZone = to;
	}

	public Player getPlayer() {
		return mPlayer;
	}

	public Zone getFromZone() {
		return mFromZone;
	}

	public Zone getToZone() {
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
