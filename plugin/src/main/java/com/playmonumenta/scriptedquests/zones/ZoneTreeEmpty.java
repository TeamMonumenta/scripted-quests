package com.playmonumenta.scriptedquests.zones;

import org.bukkit.util.Vector;

import org.dynmap.markers.MarkerSet;

public class ZoneTreeEmpty extends ZoneTreeBase {
	protected void invalidate() {
		// Nothing to do! Still needs to be a valid method, though.
	}

	public ZoneFragment getZoneFragment(Vector loc) {
		return null;
	}

	public int maxDepth() {
		return 0;
	}

	protected int totalDepth() {
		return 0;
	}

	public void refreshDynmapTree(MarkerSet markerSet, int parentR, int parentG, int parentB) {
		// Nothing to do! Still needs to be a valid method, though.
	}

	public String toString() {
		return ("ZoneTreeEmpty()");
	}
}
