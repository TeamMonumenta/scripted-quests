package com.playmonumenta.scriptedquests.zones.zonetree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.util.Vector;

import com.playmonumenta.scriptedquests.zones.zone.Zone;
import com.playmonumenta.scriptedquests.zones.zone.ZoneFragment;

public abstract class BaseZoneTree {
	public static BaseZoneTree CreateZoneTree(ArrayList<ZoneFragment> zones) {
		if (zones.size() == 0) {
			return new EmptyZoneTree();
		} else if (zones.size() == 1) {
			return new LeafZoneTree(zones.get(0));
		} else {
			return new ParentZoneTree(zones);
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
	public abstract ZoneFragment getZoneFragment(Vector loc);

	/*
	 * For a given location, return the zones that contain it.
	 */
	public HashMap<String, Zone> getZones(Vector loc) {
		ZoneFragment fragment = getZoneFragment(loc);

		if (fragment == null) {
			return null;
		}

		return fragment.getParents();
	}

	/*
	 * For a given location and layer name, return the zone that contains it.
	 * Returns null if no zone overlaps it on that layer.
	 */
	public Zone getZone(String layer, Vector loc) {
		ZoneFragment fragment = getZoneFragment(loc);

		if (fragment == null) {
			return null;
		}

		return fragment.getParent(layer);
	}
}
