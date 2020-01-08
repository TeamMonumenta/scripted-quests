package com.playmonumenta.scriptedquests.zones.zonetree;

import org.bukkit.util.Vector;

import com.playmonumenta.scriptedquests.zones.zone.ZoneFragment;

public class LeafZoneTree extends BaseZoneTree {
	private ZoneFragment mZone;

	public LeafZoneTree(ZoneFragment zone) throws Exception {
		if (zone == null) {
			throw new Exception("zone may not be null.");
		}

		mZone = zone;
	}

	public ZoneFragment getZoneFragment(Vector loc) throws Exception {
		if (mZone.within(loc)) {
			return mZone;
		} else {
			return null;
		}
	}
}
