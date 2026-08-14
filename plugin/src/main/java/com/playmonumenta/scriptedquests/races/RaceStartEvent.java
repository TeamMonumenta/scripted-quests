package com.playmonumenta.scriptedquests.races;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RaceStartEvent extends PlayerEvent {
	private static final HandlerList HANDLERS = new HandlerList();

	private final Race mRace;

	public RaceStartEvent(Player player, Race race) {
		super(player);
		mRace = race;
	}

	public Race getRace() {
		return mRace;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
