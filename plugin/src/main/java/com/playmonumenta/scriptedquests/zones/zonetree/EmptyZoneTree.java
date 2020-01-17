package com.playmonumenta.scriptedquests.zones.zonetree;

import org.bukkit.util.Vector;

import com.playmonumenta.scriptedquests.zones.zone.ZoneFragment;

public class EmptyZoneTree extends BaseZoneTree {
	/*
	 * Invalidate all fragments in the tree, causing any players inside them to be
	 * considered outside them. This updates them to the correct zone automagically.
	 */
	public void invalidate() {
		// Nothing to do! Still needs to be a valid method, though.
		return;
	}

	public ZoneFragment getZoneFragment(Vector loc) {
		return null;
	}
}
