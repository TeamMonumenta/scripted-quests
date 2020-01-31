package com.playmonumenta.scriptedquests.zones.zone;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Axis;
import org.bukkit.util.Vector;

import com.playmonumenta.scriptedquests.utils.ZoneUtils;

/*
 * A fragment of a zone; this is used to find zones quickly, but not hold their properties.
 * Instead, each fragment points to its parent, a zone with properties.
 * Each zone also keeps track of its fragments.
 */
public class ZoneFragment extends BaseZone {
	private HashMap<String, Zone> mParents = new HashMap<String, Zone>();
	private boolean mValid;

	public ZoneFragment(ZoneFragment other) {
		super(other);
		mParents.putAll(other.mParents);
		mValid = other.mValid;
	}

	public ZoneFragment(Zone other) {
		super(other);
		mParents.put(other.getLayerName(), other);
		mValid = true;
	}

	/*
	 * Returns (lower_zone, upper_zone) for this split along some axis.
	 *
	 * Either zone may have a size of 0, and should be ignored.
	 */
	private ZoneFragment[] splitAxis(Vector pos, Axis axis) {
		ZoneFragment[] result = new ZoneFragment[2];

		ZoneFragment lower = new ZoneFragment(this);
		ZoneFragment upper = new ZoneFragment(this);

		Vector lowerMax = lower.trueMaxCorner();
		Vector upperMin = upper.minCorner();

		switch(axis) {
		case X:
			lowerMax.setX(pos.getX());
			upperMin.setX(pos.getX());
			break;
		case Z:
			lowerMax.setZ(pos.getZ());
			upperMin.setZ(pos.getZ());
			break;
		default:
			lowerMax.setY(pos.getY());
			upperMin.setY(pos.getY());
		}

		lower.trueMaxCorner(lowerMax);
		upper.minCorner(upperMin);

		result[0] = lower;
		result[1] = upper;

		return result;
	}

	/*
	 * Returns a list of fragments of this zone, split by an overlapping zone.
	 * Does not include overlap or register a new parent.
	 */
	public ArrayList<ZoneFragment> splitByOverlap(BaseZone overlap) {
		return splitByOverlap(overlap, null);
	}

	/*
	 * Returns a list of fragments of this zone, split by an overlapping zone.
	 * Optionally register a new parent and return the center zone.
	 *
	 * When registering a new parent, only do so for one of the parent zones.
	 * The other parent zone should have the overlap removed as normal to avoid
	 * overlapping fragments.
	 */
	public ArrayList<ZoneFragment> splitByOverlap(BaseZone overlap, Zone newParent) {
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

				if (lower != null && lower.isValid()) {
					result.add(lower);
				}
				if (workZone != null && workZone.isValid()) {
					result.add(workZone);
				}
				if (upper != null && upper.isValid()) {
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

			if (lower != null && lower.isValid()) {
				result.add(lower);
			}
			if (upper != null && upper.isValid()) {
				result.add(upper);
			}
		}

		// If registering a new parent, it may be added now that the center zone is the size of the overlap.
		if (newParent != null) {
			centerZone.mParents.put(newParent.getLayerName(), newParent);
			result.add(centerZone);
		}

		return result;
	}

	/*
	 * Merge two ZoneFragments without changing their combined size/shape.
	 *
	 * Returns the merged ZoneFragment or None.
	 */
	public ZoneFragment merge(ZoneFragment other) {
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

	public HashMap<String, Zone> getParents() {
		HashMap<String, Zone> result = new HashMap<String, Zone>();
		result.putAll(mParents);
		return result;
	}

	public Zone getParent(String layer) {
		return mParents.get(layer);
	}

	public boolean hasProperty(String layerName, String propertyName) {
		Zone zone = getParent(layerName);
		return zone != null && zone.hasProperty(propertyName);
	}

	public void invalidate() {
		mValid = false;
	}

	@Override
	public boolean within(Vector loc) {
		if (loc == null) {
			return false;
		}

		if (!mValid) {
			return false;
		}

		for (Axis axis : Axis.values()) {
			double test = ZoneUtils.vectorAxis(loc, axis);
			double min = ZoneUtils.vectorAxis(minCorner(), axis);
			double max = ZoneUtils.vectorAxis(trueMaxCorner(), axis);
			if (test < min || test >= max) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return ("(ZoneFragment(" + mParents.toString() + ") from "
		        + minCorner().toString() + " to "
		        + maxCorner().toString() + ")");
	}
}
