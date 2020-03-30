package com.playmonumenta.scriptedquests.quests.components;

import java.util.List;

import org.bukkit.Location;

public interface QuestLocation {
	/*
	 * Returns a list of waypoints, where the last waypoint is the same locatios as getLocation()
	 * Caller must not modify this!
	 */
	List<Location> getWaypoints();

	/*
	 * Returns the last waypoint
	 * Caller must not modify this!
	 */
	Location getLocation();

	String getMessage();
}
