package com.playmonumenta.scriptedquests.zones.zonetree;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import com.playmonumenta.scriptedquests.zones.zone.Zone;
import com.playmonumenta.scriptedquests.zones.zone.ZoneFragment;

public abstract class BaseZoneTree {
	public static BaseZoneTree CreateZoneTree(ArrayList<ZoneFragment> zones) throws Exception {
		if (zones == null) {
			throw new Exception("zones may not be null.");
		}

		if (zones.size() == 0) {
			return new EmptyZoneTree();
		} else if (zones.size() == 1) {
			return new LeafZoneTree(zones.get(0));
		} else {
			return new ParentZoneTree(zones);
		}
	}

	/*
	 * For a given location, return the fragment that contains it.
	 * Returns null if no fragment overlaps it.
	 */
	public abstract ZoneFragment getZoneFragment(Vector loc);

	public ZoneFragment getZoneFragment(Location loc) {
		if (loc == null) {
			return null;
		}

		return getZoneFragment(loc.toVector());
	}

	/*
	 * For a given location, return the zone that contains it.
	 * Returns null if no zone overlaps it.
	 */
	public Zone getZone(Vector loc) {
		return getZoneFragment(loc).parent();
	}

	public Zone getZone(Location loc) {
		if (loc == null) {
			return null;
		}

		return getZone(loc.toVector());
	}
}
