package com.playmonumenta.scriptedquests.zones.zone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Axis;
import org.bukkit.util.Vector;

import com.playmonumenta.scriptedquests.utils.ZoneUtils;

/*
 * A fragment of a zone; this is used to find zones quickly, but not hold their properties.
 * Instead, each fragment points to its parent, a zone with properties.
 * Each zone also keeps track of its fragments.
 */
public class ZoneFragment<T> extends BaseZone {
	private Map<String, Zone<T>> mParents = new HashMap<String, Zone<T>>();
	private Map<String, List<Zone<T>>> mParentsAndEclipsed = new HashMap<String, List<Zone<T>>>();
	private boolean mValid;

	public ZoneFragment(ZoneFragment<T> other) {
		super(other);
		mParents.putAll(other.mParents);
		for (Map.Entry<String, List<Zone<T>>> entry : other.mParentsAndEclipsed.entrySet()) {
			mParentsAndEclipsed.put(entry.getKey(), new ArrayList<Zone<T>>(entry.getValue()));
		}
		mValid = other.mValid;
	}

	public ZoneFragment(Zone<T> other) {
		super(other);
		mParents.put(other.getLayerName(), other);
		List<Zone<T>> zones = new ArrayList<Zone<T>>();
		zones.add(other);
		mParentsAndEclipsed.put(other.getLayerName(), zones);
		mValid = true;
	}

	/*
	 * Returns (lower_zone, upper_zone) for this split along some axis.
	 *
	 * Either zone may have a size of 0, and should be ignored.
	 */
	@SuppressWarnings("unchecked")
	private ZoneFragment<T>[] splitAxis(Vector pos, Axis axis) {
		ZoneFragment<T>[] result = (ZoneFragment<T>[]) new ZoneFragment[2];

		ZoneFragment<T> lower = new ZoneFragment<T>(this);
		ZoneFragment<T> upper = new ZoneFragment<T>(this);

		Vector lowerMax = lower.maxCornerExclusive();
		Vector upperMin = upper.minCorner();

		switch (axis) {
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

		lower.maxCornerExclusive(lowerMax);
		upper.minCorner(upperMin);

		result[0] = lower;
		result[1] = upper;

		return result;
	}

	/*
	 * Returns a list of fragments of this zone, split by an overlapping zone.
	 * Does not include overlap or register a new parent.
	 */
	public List<ZoneFragment<T>> splitByOverlap(BaseZone overlap, Zone<T> newParent) {
		return splitByOverlap(overlap, newParent, false);
	}

	/*
	 * Returns a list of fragments of this zone, split by an overlapping zone.
	 * Optionally register a new parent and return the center zone.
	 *
	 * When registering a new parent, only do so for one of the parent zones.
	 * The other parent zone should have the overlap removed as normal to avoid
	 * overlapping fragments.
	 */
	public List<ZoneFragment<T>> splitByOverlap(BaseZone overlap, Zone<T> newParent, boolean includeOverlap) {
		ZoneFragment<T> centerZone = new ZoneFragment<T>(this);

		Vector otherMin = overlap.minCorner();
		Vector otherMax = overlap.maxCornerExclusive();

		ZoneFragment<T>[] tempSplitResult;
		List<ZoneFragment<T>> result = new ArrayList<ZoneFragment<T>>();

		for (Axis axis : Axis.values()) {
			ZoneFragment<T> lower;
			ZoneFragment<T> upper;

			List<ZoneFragment<T>> workZones = result;
			result = new ArrayList<ZoneFragment<T>>();

			for (ZoneFragment<T> workZone : workZones) {
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

		// If the center fragment is kept, the original parents take priority over the new parent
		// Failing to do this would mean the fragment takes priority from the wrong zone
		String newParentLayer = newParent.getLayerName();
		if (!centerZone.mParents.containsKey(newParentLayer)) {
			centerZone.mParents.put(newParentLayer, newParent);
		}

		// Track the new parent zone of the center fragment, even if it's eclipsed.
		List<Zone<T>> newParentLayerZones = centerZone.mParentsAndEclipsed.get(newParentLayer);
		if (newParentLayerZones == null) {
			newParentLayerZones = new ArrayList<Zone<T>>();
			centerZone.mParentsAndEclipsed.put(newParentLayer, newParentLayerZones);
		}
		newParentLayerZones.add(newParent);

		// If registering a new parent, it may be added now that the center zone is the size of the overlap.
		if (includeOverlap) {
			result.add(centerZone);
		}

		return result;
	}

	/*
	 * Merge two ZoneFragments without changing their combined size/shape.
	 *
	 * Assumes fragments do not overlap.
	 *
	 * Returns the merged ZoneFragment or None.
	 */
	public ZoneFragment<T> merge(ZoneFragment<T> other) {
		if (mValid != other.mValid ||
		    !mParents.equals(other.mParents) ||
		    !mParentsAndEclipsed.equals(other.mParentsAndEclipsed)) {
			return null;
		}

		Vector aMin = minCorner();
		Vector aMax = maxCornerExclusive();
		Vector bMin = other.minCorner();
		Vector bMax = other.maxCornerExclusive();

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
			// These are the same zone; return clone of self.
			return new ZoneFragment<T>(this);
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
		ZoneFragment<T> result;
		try {
			result = new ZoneFragment<T>(this);
		} catch (Exception e) {
			throw e;
		}

		Vector resultMin = Vector.getMinimum(aMin, bMin);
		Vector resultMax = Vector.getMaximum(aMax, bMax);
		result.minCorner(resultMin);
		result.maxCornerExclusive(resultMax);

		return result;
	}

	public Map<String, Zone<T>> getParents() {
		Map<String, Zone<T>> result = new HashMap<String, Zone<T>>();
		result.putAll(mParents);
		return result;
	}

	public Zone<T> getParent(String layer) {
		return mParents.get(layer);
	}

	public Map<String, List<Zone<T>>> getParentsAndEclipsed() {
		Map<String, List<Zone<T>>> result = new HashMap<String, List<Zone<T>>>();
		for (Map.Entry<String, List<Zone<T>>> entry : mParentsAndEclipsed.entrySet()) {
			String layerName = entry.getKey();
			List<Zone<T>> zones = entry.getValue();

			List<Zone<T>> resultZones = new ArrayList<Zone<T>>();
			resultZones.addAll(zones);
			result.put(layerName, resultZones);
		}
		return result;
	}

	public List<Zone<T>> getParentAndEclipsed(String layer) {
		List<Zone<T>> zones = mParentsAndEclipsed.get(layer);
		List<Zone<T>> result = new ArrayList<Zone<T>>();
		if (zones != null) {
			result.addAll(zones);
		}
		return result;
	}

	public boolean hasProperty(String layerName, String propertyName) {
		Zone<T> zone = getParent(layerName);
		return zone != null && zone.hasProperty(propertyName);
	}

	/*
	 * Force all future tests for locations within this zone to return false.
	 *
	 * This means any code tracking previous fragments/zones will be forced to check again when reloading zones.
	 */
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
			double max = ZoneUtils.vectorAxis(maxCornerExclusive(), axis);
			if (test < min || test >= max) {
				return false;
			}
		}
		return true;
	}

	public boolean equals(ZoneFragment<T> other) {
		return (super.equals(other) &&
		        mParents.equals(other.mParents) &&
		        mParentsAndEclipsed.equals(other.mParentsAndEclipsed));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31*result + mParents.hashCode();
		result = 31*result + mParentsAndEclipsed.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return ("(ZoneFragment(" + mParents.toString() + ") from "
		        + minCorner().toString() + " to "
		        + maxCorner().toString() + ")");
	}
}
