package com.playmonumenta.scriptedquests.zones;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.playmonumenta.scriptedquests.zones.zone.Zone;

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

		String layerStr = "<unknown layer>";

		String fromStr = "null";
		if (from != null) {
			layerStr = from.getLayerName();
			fromStr = from.getName();
		}

		String toStr = "null";
		if (to != null) {
			layerStr = to.getLayerName();
			toStr = to.getName();
		}
	}

	public Player getPlayer() {
		return mPlayer;
	}

	public String getLayer() {
		return mLayer;
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
