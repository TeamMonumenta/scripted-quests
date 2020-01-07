package com.playmonumenta.scriptedquests.zones.zone;

import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.Axis;
import org.bukkit.util.Vector;

/*
 * A fragment of a zone; this is used to find zones quickly, but not hold their properties.
 * Instead, each fragment points to its parent, a zone with properties.
 * Each zone also keeps track of its fragments.
 */
public class Zone extends BaseZone {
	private String mName;
	private ArrayList<ZoneFragment> mFragments = new ArrayList<ZoneFragment>();

	/*
	 * pos1 and pos2 are used similar to /fill:
	 * - Both are inclusive coordinates.
	 * - The minimum/maximum are determined for you.
	 */
	public Zone(Vector pos1, Vector pos2, String name) throws Exception {
		super(pos1, pos2);

		if (name == null) {
			throw new Exception("name may not be null.");
		}
		mName = name;

		ZoneFragment initialFragment = new ZoneFragment(this);
		mFragments.add(initialFragment);
	}

	/*
	 * Split all fragments of this zone by an overlapping zone, removing overlap.
	 */
	public void splitByOverlap(BaseZone overlap) throws Exception {
		ArrayList<ZoneFragment> newFragments = new ArrayList<ZoneFragment>();
		for (ZoneFragment fragment : mFragments) {
			BaseZone subOverlap = fragment.overlappingZone(overlap);

			if (subOverlap == null)
			{
				newFragments.add(fragment);
				continue;
			}

			newFragments.addAll(fragment.splitByOverlap(subOverlap));
		}
		mFragments = newFragments;
	}
}
