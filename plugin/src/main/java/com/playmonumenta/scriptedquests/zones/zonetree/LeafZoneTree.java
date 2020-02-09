package com.playmonumenta.scriptedquests.zones.zonetree;

import org.bukkit.util.Vector;

import com.playmonumenta.scriptedquests.zones.zone.ZoneFragment;

public class LeafZoneTree<T> extends BaseZoneTree<T> {
	private ZoneFragment<T> mZone;

	public LeafZoneTree(ZoneFragment<T> zone) {
		mZone = zone;
	}

	public void invalidate() {
		mZone.invalidate();
	}

	public ZoneFragment<T> getZoneFragment(Vector loc) {
		if (mZone.within(loc)) {
			return mZone;
		} else {
			return null;
		}
	}

	public String toString() {
		return ("BaseZoneTree(" + mZone.toString() + ")");
	}
}
