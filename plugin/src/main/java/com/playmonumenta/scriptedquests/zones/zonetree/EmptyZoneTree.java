package com.playmonumenta.scriptedquests.zones.zonetree;

import org.bukkit.util.Vector;

import org.dynmap.markers.MarkerSet;

import com.playmonumenta.scriptedquests.zones.zone.ZoneFragment;

public class EmptyZoneTree<T> extends BaseZoneTree<T> {
	public void invalidate() {
		// Nothing to do! Still needs to be a valid method, though.
		return;
	}

	public ZoneFragment<T> getZoneFragment(Vector loc) {
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
		return;
	}

	public String toString() {
		return ("EmptyZoneTree()");
	}
}
