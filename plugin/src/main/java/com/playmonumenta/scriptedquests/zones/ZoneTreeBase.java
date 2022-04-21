package com.playmonumenta.scriptedquests.zones;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

public abstract class ZoneTreeBase {
	protected int mFragmentCount = 0;

	protected static ZoneTreeBase createZoneTree(List<ZoneFragment> zones) throws Exception {
		ZoneTreeBase result;
		if (zones.size() == 0) {
			result = new ZoneTreeEmpty();
		} else if (zones.size() == 1) {
			result = new ZoneTreeLeaf(zones.get(0));
		} else {
			result = new ZoneTreeParent(zones);
		}

		return result;
	}

	/*
	 * Invalidate all fragments in the tree, causing any players inside them to be
	 * considered outside them. This updates them to the correct zone automagically.
	 */
	protected abstract void invalidate();

	/*
	 * Returns all ZoneFragments that overlap a bounding box.
	 */
	public abstract Set<ZoneFragment> getZoneFragments(BoundingBox bb);

	/*
	 * For a given location, return the fragment that contains it.
	 * Returns null if no fragment overlaps it.
	 */
	public abstract @Nullable ZoneFragment getZoneFragment(Vector loc);

	/*
	 * For a given location, return the zones that contain it.
	 */
	public Map<String, Zone> getZones(Vector loc) {
		@Nullable ZoneFragment fragment = getZoneFragment(loc);

		if (fragment == null) {
			return new HashMap<String, Zone>();
		}

		return fragment.getParents();
	}

	/*
	 * Returns all zones that overlap a bounding box, optionally including eclipsed zones.
	 */
	public Set<Zone> getZones(BoundingBox bb, boolean includeEclipsed) {
		Set<Zone> result = new HashSet<>();
		for (ZoneFragment fragment : getZoneFragments(bb)) {
			if (includeEclipsed) {
				for (List<Zone> zones : fragment.getParentsAndEclipsed().values()) {
					result.addAll(zones);
				}
			} else {
				result.addAll(fragment.getParents().values());
			}
		}
		return result;
	}

	/*
	 * For a given location and layer name, return the zone that contains it.
	 * Returns null if no zone overlaps it on that layer.
	 */
	public @Nullable Zone getZone(Vector loc, String layer) {
		@Nullable ZoneFragment fragment = getZoneFragment(loc);

		if (fragment == null) {
			return null;
		}

		return fragment.getParent(layer);
	}

	public boolean hasProperty(Vector loc, String layerName, String propertyName) {
		@Nullable ZoneFragment fragment = getZoneFragment(loc);
		return fragment != null && fragment.hasProperty(layerName, propertyName);
	}

	public int fragmentCount() {
		return mFragmentCount;
	}

	public abstract int maxDepth();

	protected abstract int totalDepth();

	public float averageDepth() {
		if (mFragmentCount == 0) {
			return 0.0f;
		} else {
			return (float) totalDepth() / (float) mFragmentCount;
		}
	}

	public void refreshDynmapTree() {
		@Nullable DynmapCommonAPI dynmapHook = (DynmapCommonAPI) Bukkit.getServer().getPluginManager().getPlugin("dynmap");
		if (dynmapHook == null) {
			return;
		}

		@Nullable MarkerAPI markerHook = dynmapHook.getMarkerAPI();
		if (markerHook == null) {
			// Not initialized
			return;
		}

		String markerSetId = ZoneLayer.DYNMAP_PREFIX + "Tree";
		@Nullable MarkerSet markerSet = markerHook.getMarkerSet(markerSetId);
		if (markerSet != null) {
			// Delete old marker set
			markerSet.deleteMarkerSet();
		}
		// Create a new marker set
		markerSet = markerHook.createMarkerSet(markerSetId, "Zone Tree", null, false);
		markerSet.setHideByDefault(true);

		refreshDynmapTree(markerSet, 0, 0, 0);
	}

	protected abstract void refreshDynmapTree(MarkerSet markerSet, int parentR, int parentG, int parentB);
}
