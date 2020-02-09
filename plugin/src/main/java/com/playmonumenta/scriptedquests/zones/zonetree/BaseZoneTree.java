package com.playmonumenta.scriptedquests.zones.zonetree;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.util.Vector;

import org.dynmap.DynmapCommonAPI;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.zones.ZoneLayer;
import com.playmonumenta.scriptedquests.zones.zone.Zone;
import com.playmonumenta.scriptedquests.zones.zone.ZoneFragment;

public abstract class BaseZoneTree<T> {
	protected int mFragmentCount = 0;

	public static <T> BaseZoneTree<T> CreateZoneTree(Plugin plugin, CommandSender sender, ArrayList<ZoneFragment<T>> zones) throws Exception {
		BaseZoneTree result;
		if (zones.size() == 0) {
			result = new EmptyZoneTree<T>();
		} else if (zones.size() == 1) {
			result = new LeafZoneTree<T>(zones.get(0));
		} else {
			result = new ParentZoneTree<T>(plugin, sender, zones);

		if (plugin.mShowZonesDynmap) {
			result.refreshDynmapTree();
		}

		return result;
	}

	/*
	 * Invalidate all fragments in the tree, causing any players inside them to be
	 * considered outside them. This updates them to the correct zone automagically.
	 */
	public abstract void invalidate();

	/*
	 * For a given location, return the fragment that contains it.
	 * Returns null if no fragment overlaps it.
	 */
	public abstract ZoneFragment<T> getZoneFragment(Vector loc);

	/*
	 * For a given location, return the zones that contain it.
	 */
	public HashMap<String, Zone<T>> getZones(Vector loc) {
		ZoneFragment<T> fragment = getZoneFragment(loc);

		if (fragment == null) {
			return null;
		}

		return fragment.getParents();
	}

	/*
	 * For a given location and layer name, return the zone that contains it.
	 * Returns null if no zone overlaps it on that layer.
	 */
	public Zone<T> getZone(Vector loc, String layer) {
		ZoneFragment<T> fragment = getZoneFragment(loc);

		if (fragment == null) {
			return null;
		}

		return fragment.getParent(layer);
	}

	public boolean hasProperty(Vector loc, String layerName, String propertyName) {
		ZoneFragment<T> fragment = getZoneFragment(loc);
		return fragment != null && fragment.hasProperty(layerName, propertyName);
	}

	public int fragmentCount() {
		return mFragmentCount;
	}

	public void refreshDynmapTree() {
		DynmapCommonAPI dynmapHook = (DynmapCommonAPI) Bukkit.getServer().getPluginManager().getPlugin("dynmap");
		if (dynmapHook == null) {
			return;
		}

		MarkerAPI markerHook = dynmapHook.getMarkerAPI();
		if (markerHook == null) {
			// Not initialized
			return;
		}

		String markerSetId = ZoneLayer.DYNMAP_PREFIX + "Tree";
		MarkerSet markerSet;

		markerSet = markerHook.getMarkerSet(markerSetId);
		if (markerSet != null) {
			// Delete old marker set
			markerSet.deleteMarkerSet();
		}
		// Create a new marker set
		markerSet = markerHook.createMarkerSet(markerSetId, "Zone Tree", null, false);
		markerSet.setHideByDefault(true);

		refreshDynmapTree(markerSet, 0, 0, 0);
	}

	public abstract void refreshDynmapTree(MarkerSet markerSet, int parentR, int parentG, int parentB);
}
