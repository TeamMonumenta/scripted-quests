package com.playmonumenta.scriptedquests.zones.zonetree;

import org.bukkit.util.Vector;

import com.playmonumenta.scriptedquests.zones.zone.ZoneFragment;

public class EmptyZoneTree<T> extends BaseZoneTree<T> {
	public void invalidate() {
		// Nothing to do! Still needs to be a valid method, though.
		return;
	}

	public ZoneFragment<T> getZoneFragment(Vector loc) {
		return null;
	}

	public String toString() {
		return ("EmptyZoneTree()");
	}
}
