package com.playmonumenta.scriptedquests.races;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class RaceWorldRecordEvent extends PlayerEvent {
	private static final HandlerList HANDLERS = new HandlerList();

	private final Race mRace;
	private final boolean mIsSpeedRecord;
	private final int mValue;

	public RaceWorldRecordEvent(Player player, Race race, boolean isSpeedRecord, int value) {
		super(player);
		mRace = race;
		mIsSpeedRecord = isSpeedRecord;
		mValue = value;
	}

	public Race getRace() {
		return mRace;
	}

	public boolean isSpeedRecord() {
		return mIsSpeedRecord;
	}

	public int getValue() {
		return mValue;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
