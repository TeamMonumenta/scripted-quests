package com.playmonumenta.scriptedquests.zones;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerSet;

public class ZoneTreeLeaf extends ZoneTreeBase {
	private ZoneFragment mFragment;

	public ZoneTreeLeaf(ZoneFragment zone) {
		mFragmentCount = 1;
		mFragment = zone;
	}

	@Override
	protected void invalidate() {
		mFragment.invalidate();
	}

	@Override
	public Set<ZoneFragment> getZoneFragments(BoundingBox bb) {
		Set<ZoneFragment> result = new HashSet<>();
		if (mFragment.boundingBox().overlaps(bb)) {
			result.add(mFragment);
		}
		return result;
	}

	@Override
	public @Nullable ZoneFragment getZoneFragment(Vector loc) {
		if (mFragment.within(loc)) {
			return mFragment;
		} else {
			return null;
		}
	}

	@Override
	public int maxDepth() {
		return 1;
	}

	@Override
	protected int totalDepth() {
		return 1;
	}

	@Override
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

		int color;
		color = (r & 0xff);
		color = (color << 8) | (g & 0xff);
		color = (color << 8) | (b & 0xff);

		AreaMarker areaMarker = markerSet.createAreaMarker(fragmentId, fragmentLabel, false, world, x, z, false);
		areaMarker.setRangeY(maxCorner.getY(), minCorner.getY());
		areaMarker.setFillStyle(0.2, color);
		areaMarker.setLineStyle(1, 0.3, color);
	}

	@Override
	public String toString() {
		return ("ZoneTreeBase(" + mFragment.toString() + ")");
	}
}
