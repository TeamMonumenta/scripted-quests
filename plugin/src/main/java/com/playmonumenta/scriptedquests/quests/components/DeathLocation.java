package com.playmonumenta.scriptedquests.quests.components;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;

public class DeathLocation implements QuestLocation {
	private final long mDeathTime;
	private final List<Location> mWaypoints = new ArrayList<>(1);

	public DeathLocation(Location loc, long deathTime) {
		mDeathTime = deathTime;
		mWaypoints.add(loc);
	}

	public String getTimeDifference(long compareTime) {
		final long diff = compareTime - mDeathTime;
		final long diffSeconds = diff / 1000 % 60;
		final long diffMinutes = diff / (60 * 1000) % 60;
		final long diffHours = diff / (60 * 60 * 1000) % 24;
		final long diffDays = diff / (24 * 60 * 60 * 1000);

		String timeStr = "";
		if (diffDays > 0) {
			timeStr += diffDays + " day";
			if (diffDays > 1) {
				timeStr += "s";
			}
		}

		if (diffDays > 0 && (diffHours > 0 || diffMinutes > 0 || diffSeconds > 0)) {
			timeStr += " ";
		}

		if (diffHours > 0) {
			timeStr += diffHours + " hour";
			if (diffHours > 1) {
				timeStr += "s";
			}
		}

		if (diffHours > 0 && (diffMinutes > 0 || diffSeconds > 0)) {
			timeStr += " ";
		}

		if (diffMinutes > 0) {
			timeStr += diffMinutes + " minute";
			if (diffMinutes > 1) {
				timeStr += "s";
			}
		}

		if (diffMinutes > 0 && diffSeconds > 0) {
			timeStr += " ";
		}

		//If it has been less than a second, say 0 seconds, but don't list seconds if it is an exact number of minutes
		if (diffSeconds > 0 || diffMinutes == 0) {
			timeStr += diffSeconds + " second";
			if (diffSeconds != 1) {
				timeStr += "s";
			}
		}

		return timeStr;
	}

	@Override
	public List<Location> getWaypoints() {
		return mWaypoints;
	}

	@Override
	public Location getLocation() {
		return mWaypoints.get(0);
	}

	@Override
	public String getMessage() {
		return getTimeDifference(System.currentTimeMillis()) + " ago";
	}
}
