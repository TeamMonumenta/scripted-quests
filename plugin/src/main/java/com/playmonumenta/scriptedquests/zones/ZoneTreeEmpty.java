package com.playmonumenta.scriptedquests.zones;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.dynmap.markers.MarkerSet;

public class ZoneTreeEmpty extends ZoneTreeBase {
	protected void invalidate() {
		// Nothing to do! Still needs to be a valid method, though.
	}

	public Set<ZoneFragment> getZoneFragments(BoundingBox bb) {
		return new HashSet<>();
	}

	public @Nullable ZoneFragment getZoneFragment(Vector loc) {
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
