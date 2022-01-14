package com.playmonumenta.scriptedquests.zones;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.dynmap.markers.MarkerSet;
import org.dynmap.markers.AreaMarker;

public class ZoneTreeLeaf extends ZoneTreeBase {
	private ZoneFragment mFragment;

	public ZoneTreeLeaf(ZoneFragment zone) {
		mFragmentCount = 1;
		mFragment = zone;
	}

	protected void invalidate() {
		mFragment.invalidate();
	}

	public Set<ZoneFragment> getZoneFragments(BoundingBox bb) {
		Set<ZoneFragment> result = new HashSet<>();
		if (mFragment.boundingBox().contains(bb)) {
			result.add(mFragment);
		}
		return result;
	}

	public @Nullable ZoneFragment getZoneFragment(Vector loc) {
		if (mFragment.within(loc)) {
			return mFragment;
		} else {
			return null;
		}
	}

	public int maxDepth() {
		return 1;
	}

	protected int totalDepth() {
		return 1;
	}

	protected void refreshDynmapTree(MarkerSet markerSet, int parentR, int parentG, int parentB) {
		int r = parentR;
		int g = parentG;
		int b = parentB;

		if (r == 0 && g == 0 && b == 0) {
			r = 0x7f;
			g = 0x7f;
			b = 0x7f;
		}

		String world = Bukkit.getWorlds().get(0).getName();
		String fragmentId = "zone_fragment_hash_" + Integer.toString(hashCode());
		String fragmentLabel = "zone fragment";

		Vector minCorner = mFragment.minCorner();
		Vector maxCorner = mFragment.maxCornerExclusive();

		double[] x = new double[2];
		double[] z = new double[2];
		x[0] = minCorner.getX();
		z[0] = minCorner.getZ();
		x[1] = maxCorner.getX();
		z[1] = maxCorner.getZ();

		int color = 0;
		color = (r & 0xff);
		color = (color << 8) | (g & 0xff);
		color = (color << 8) | (b & 0xff);

		AreaMarker areaMarker = markerSet.createAreaMarker(fragmentId, fragmentLabel, false, world, x, z, false);
		areaMarker.setRangeY(maxCorner.getY(), minCorner.getY());
		areaMarker.setFillStyle(0.2, color);
		areaMarker.setLineStyle(1, 0.3, color);
	}

	public String toString() {
		return ("ZoneTreeBase(" + mFragment.toString() + ")");
	}
}
