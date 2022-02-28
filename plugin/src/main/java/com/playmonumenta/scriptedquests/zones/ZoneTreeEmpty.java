package com.playmonumenta.scriptedquests.zones;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import javax.annotation.Nullable;
import org.dynmap.markers.MarkerSet;

public class ZoneTreeEmpty extends ZoneTreeBase {
	@Override
	protected void invalidate() {
		// Nothing to do! Still needs to be a valid method, though.
	}

	@Override
	public Set<ZoneFragment> getZoneFragments(BoundingBox bb) {
		return new HashSet<>();
	}

	@Override
	public @Nullable ZoneFragment getZoneFragment(Vector loc) {
		return null;
	}

	@Override
	public int maxDepth() {
		return 0;
	}

	@Override
	protected int totalDepth() {
		return 0;
	}

	@Override
	public void refreshDynmapTree(MarkerSet markerSet, int parentR, int parentG, int parentB) {
		// Nothing to do! Still needs to be a valid method, though.
	}

	@Override
	public String toString() {
		return "ZoneTreeEmpty()";
	}
}
