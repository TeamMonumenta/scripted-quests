package com.playmonumenta.scriptedquests.zones.zonetree;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.util.Vector;

import com.playmonumenta.scriptedquests.zones.zone.Zone;
import com.playmonumenta.scriptedquests.zones.zone.ZoneFragment;

public abstract class BaseZoneTree<T> {
	public static <T> BaseZoneTree<T> CreateZoneTree(CommandSender sender, ArrayList<ZoneFragment<T>> zones) throws Exception {
		if (zones.size() == 0) {
			return new EmptyZoneTree<T>();
		} else if (zones.size() == 1) {
			return new LeafZoneTree<T>(zones.get(0));
		} else {
			return new ParentZoneTree<T>(sender, zones);
		}
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
}
