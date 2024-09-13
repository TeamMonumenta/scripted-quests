package com.playmonumenta.scriptedquests.zones;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

public abstract class ZoneTreeBase {
	protected int mFragmentCount = 0;

	protected static ZoneTreeBase createZoneTree(List<ZoneFragment> zones) throws Exception {
		ZoneTreeBase result;
		if (zones.isEmpty()) {
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
	public Map<String, Zone> getZones(Location location) {
		return getZones(location, null);
	}

	public Map<String, Zone> getZones(Location location, @Nullable CommandSender sender) {
		@Nullable ZoneFragment fragment = getZoneFragment(location.toVector());

		if (fragment == null) {
			return new HashMap<>();
		}

		return fragment.getParents(location.getWorld(), sender);
	}

	// Avoid using this except for test code; searching by world is more efficient
	public Map<String, Zone> getZones(String worldName, Vector loc) {
		Map<String, Zone> result = new HashMap<>();

		@Nullable ZoneFragment fragment = getZoneFragment(loc);

		if (fragment == null) {
			return result;
		}

		for (Map.Entry<String, List<Zone>> parentsEntry : fragment.getParents().entrySet()) {
			String namespace = parentsEntry.getKey();
			for (Zone parent : parentsEntry.getValue()) {
				if (parent.matchesWorld(worldName)) {
					result.put(namespace, parent);
					break;
				}
			}
		}

		return result;
	}

	/*
	 * Returns all zones that overlap a bounding box, optionally including eclipsed zones.
	 */
	public Set<Zone> getZones(World world, BoundingBox bb, boolean includeEclipsed) {
		Set<Zone> result = new HashSet<>();
		for (ZoneFragment fragment : getZoneFragments(bb)) {
			if (includeEclipsed) {
				for (List<Zone> zones : fragment.getParentsAndEclipsed(world).values()) {
					result.addAll(zones);
				}
			} else {
				result.addAll(fragment.getParents(world).values());
			}
		}
		return result;
	}

	/*
	 * For a given location and namespace name, return the zone that contains it.
	 * Returns null if no zone overlaps it on that namespace.
	 */
	public @Nullable Zone getZone(Location loc, String namespaceName) {
		@Nullable ZoneFragment fragment = getZoneFragment(loc.toVector());

		if (fragment == null) {
			return null;
		}

		World world = loc.getWorld();
		for (Zone zone : fragment.getParentAndEclipsed(namespaceName)) {
			if (zone.matchesWorld(world)) {
				return zone;
			}
		}
		return null;
	}

	public boolean hasProperty(Location loc, String namespaceName, String propertyName) {
		@Nullable ZoneFragment fragment = getZoneFragment(loc.toVector());
		return fragment != null && fragment.hasProperty(loc.getWorld(), namespaceName, propertyName);
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

		String markerSetId = ZoneNamespace.DYNMAP_PREFIX + "Tree";
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

	public void print(PrintStream out) {
		print(out, "");
	}

	protected abstract void print(PrintStream out, String indentation);

}
