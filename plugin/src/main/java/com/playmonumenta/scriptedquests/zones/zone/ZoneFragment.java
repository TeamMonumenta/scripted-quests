package com.playmonumenta.scriptedquests.zones.zone;

import java.util.ArrayList;

import org.bukkit.Axis;
import org.bukkit.util.Vector;

/*
 * A fragment of a zone; this is used to find zones quickly, but not hold their properties.
 * Instead, each fragment points to its parent, a zone with properties.
 * Each zone also keeps track of its fragments.
 */
public class ZoneFragment extends BaseZone {
	private Zone mParent;

	public ZoneFragment(ZoneFragment other) throws Exception {
		super(other);
		mParent = other.mParent;
	}

	public ZoneFragment(Zone other) throws Exception {
		super(other);
		mParent = other;
	}

	@Override
	public ZoneFragment clone() throws CloneNotSupportedException {
		try {
			return new ZoneFragment(this);
		} catch (CloneNotSupportedException e) {
			throw new Error(e);
		} catch (Exception e) {
			throw new CloneNotSupportedException("Cannot clone null object.");
		}
	}

	/*
	 * Returns (lower_zone, upper_zone) for this split along some axis.
	 *
	 * Either zone may have a size of 0, and should be ignored.
	 */
	public ZoneFragment[] splitAxis(Vector pos, Axis axis) throws Exception {
		ZoneFragment[] result = new ZoneFragment[2];

		ZoneFragment lower;
		ZoneFragment upper;

		try {
			lower = new ZoneFragment(this);
			upper = new ZoneFragment(this);
		} catch (Exception e) {
			throw e;
		}

		Vector lowerMax = lower.maxCorner();
		Vector upperMin = upper.minCorner();

		switch(axis) {
		case X:
			lowerMax.setX(pos.getX());
			upperMin.setX(pos.getX());
			break;
		case Z:
			lowerMax.setX(pos.getX());
			upperMin.setX(pos.getX());
			break;
		default:
			lowerMax.setX(pos.getX());
			upperMin.setX(pos.getX());
		}

		lower.maxCorner(lowerMax);
		upper.minCorner(upperMin);

		result[0] = lower;
		result[1] = upper;

		return result;
	}

	/*
	 * Returns a list of fragments of this zone, split by an overlapping zone.
	 */
	public ArrayList<ZoneFragment> splitByOverlap(BaseZone overlap) throws Exception {
		if (overlap == null) {
			throw new Exception("overlap may not be null.");
		}

		ZoneFragment centerZone = new ZoneFragment(this);

		Vector otherMin = overlap.minCorner();
		Vector otherMax = overlap.trueMaxCorner();

		ZoneFragment[] tempSplitResult;
		ArrayList<ZoneFragment> result = new ArrayList<ZoneFragment>();

		for (Axis axis : Axis.values()) {
			ZoneFragment lower;
			ZoneFragment upper;

			ArrayList<ZoneFragment> workZones = result;
			result = new ArrayList<ZoneFragment>();

			for (ZoneFragment workZone : workZones) {
				// Add zones split from existing split zones
				tempSplitResult = workZone.splitAxis(otherMin, axis);
				lower = tempSplitResult[0];
				workZone = tempSplitResult[1];

				tempSplitResult = workZone.splitAxis(otherMax, axis);
				workZone = tempSplitResult[0];
				upper = tempSplitResult[1];

				if (lower != null) {
					result.add(lower);
				}
				if (workZone != null) {
					result.add(workZone);
				}
				if (upper != null) {
					result.add(upper);
				}
			}

			// Add zones split from center, but not the center (overlap) itself
			tempSplitResult = centerZone.splitAxis(otherMin, axis);
			lower = tempSplitResult[0];
			centerZone = tempSplitResult[1];

			tempSplitResult = centerZone.splitAxis(otherMax, axis);
			centerZone = tempSplitResult[0];
			upper = tempSplitResult[1];

			if (lower != null) {
				result.add(lower);
			}
			if (upper != null) {
				result.add(upper);
			}
		}

		return result;
	}

	/*
	 * Merge two ZoneFragments without changing their combined size/shape.
	 *
	 * Returns the merged ZoneFragment or None.
	 */
	public ZoneFragment merge(ZoneFragment other) throws Exception {
		Vector aMin = minCorner();
		Vector aMax = maxCorner();
		Vector bMin = other.minCorner();
		Vector bMax = other.maxCorner();

		boolean xMatches = aMin.getX() == bMin.getX() && aMax.getX() == bMax.getX();
		boolean yMatches = aMin.getY() == bMin.getY() && aMax.getY() == bMax.getY();
		boolean zMatches = aMin.getZ() == bMin.getZ() && aMax.getZ() == bMax.getZ();

		// Confirm at least 2/3 axes match
		int matchingAxes = 0;
		matchingAxes += xMatches ? 1 : 0;
		matchingAxes += yMatches ? 1 : 0;
		matchingAxes += zMatches ? 1 : 0;
		if (matchingAxes < 2) {
			// Cannot merge, return null to indicate this.
			return null;
		}
		if (matchingAxes == 3) {
			// These are the same zone; return self.
			return this;
		}

		// Confirm zone fragments touch on mismatched axis
		if (!xMatches) {
			if (aMax.getX() != bMin.getX() && bMax.getX() != aMin.getX()) {
				return null;
			}
		} else if (!yMatches) {
			if (aMax.getY() != bMin.getY() && bMax.getY() != aMin.getY()) {
				return null;
			}
		} else {
			if (aMax.getZ() != bMin.getZ() && bMax.getZ() != aMin.getZ()) {
				return null;
			}
		}

		// Merging is possible, go for it.
		ZoneFragment result;
		try {
			result = new ZoneFragment(this);
		} catch (Exception e) {
			throw e;
		}

		Vector resultMin = Vector.getMinimum(aMin, bMin);
		Vector resultMax = Vector.getMaximum(aMax, bMax);
		result.minCorner(resultMin);
		result.maxCorner(resultMax);

		return result;
	}

	public Zone parent() {
		return mParent;
	}
}
