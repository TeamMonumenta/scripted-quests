package com.playmonumenta.scriptedquests.zones.zonetree;

import org.bukkit.util.Vector;

import com.playmonumenta.scriptedquests.zones.zone.ZoneFragment;

public class LeafZoneTree extends BaseZoneTree {
	private ZoneFragment mZone;

	public LeafZoneTree(ZoneFragment zone) {
		mZone = zone;
	}

	/*
	 * Invalidate all fragments in the tree, causing any players inside them to be
	 * considered outside them. This updates them to the correct zone automagically.
	 */
	public void invalidate() {
		mZone.invalidate();
	}

	public ZoneFragment getZoneFragment(Vector loc) {
		if (mZone.within(loc)) {
			return mZone;
		} else {
			return null;
		}
	}
}
