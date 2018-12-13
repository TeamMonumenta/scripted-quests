package com.playmonumenta.scriptedquests.quests.components;

import org.bukkit.Location;

public class DeathLocation implements CompassLocation{
	private Location mLoc;
	private long mDeathTime;

	public DeathLocation(Location loc, long deathTime) {
		mLoc = loc.clone();
		mDeathTime = deathTime;
	}

	public String getTimeDifference(long compareTime) {
		long diff = compareTime - mDeathTime;

		long diffSeconds = diff / 1000 % 60;
		long diffMinutes = diff / (60 * 1000) % 60;
		long diffHours = diff / (60 * 60 * 1000) % 24;
		long diffDays = diff / (24 * 60 * 60 * 1000);

		String timeStr = "";
		if (diffDays > 0) {
			timeStr += Long.toString(diffDays) + " day";
			if (diffDays > 1) {
				timeStr += "s";
			}
		}

		if (diffDays > 0 && (diffHours > 0 || diffMinutes > 0 || diffSeconds > 0)) {
			timeStr += " ";
		}

		if (diffHours > 0) {
			timeStr += Long.toString(diffHours) + " hour";
			if (diffHours > 1) {
				timeStr += "s";
			}
		}

		if (diffHours > 0 && (diffMinutes > 0 || diffSeconds > 0)) {
			timeStr += " ";
		}

		if (diffMinutes > 0) {
			timeStr += Long.toString(diffMinutes) + " minute";
			if (diffMinutes > 1) {
				timeStr += "s";
			}
		}

		if (diffMinutes > 0 && diffSeconds > 0) {
			timeStr += " ";
		}

		if (diffSeconds > 0) {
			timeStr += Long.toString(diffSeconds) + " second";
			if (diffSeconds > 1) {
				timeStr += "s";
			}
		}

		return timeStr;
	}

	@Override
	public Location getLocation() {
		return mLoc;
	}

	@Override
	public String getMessage() {
		return getTimeDifference(System.currentTimeMillis()) + " ago";
	}
}
