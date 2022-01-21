package com.playmonumenta.scriptedquests.zones;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.playmonumenta.scriptedquests.utils.VectorUtils;

import org.bukkit.Axis;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

/*
 * A fragment of a zone; this is used to find zones quickly, but not hold their properties.
 * Instead, each fragment points to its parent, a zone with properties.
 * Each zone also keeps track of its fragments.
 */
public class ZoneFragment extends ZoneBase {
	private Map<String, Zone> mParents = new HashMap<String, Zone>();
	private Map<String, List<Zone>> mParentsAndEclipsed = new HashMap<String, List<Zone>>();
	private boolean mValid;

	protected ZoneFragment(ZoneFragment other) {
		super(other);
		mParents.putAll(other.mParents);
		for (Map.Entry<String, List<Zone>> entry : other.mParentsAndEclipsed.entrySet()) {
			mParentsAndEclipsed.put(entry.getKey(), new ArrayList<Zone>(entry.getValue()));
		}
		mValid = other.mValid;
	}

	protected ZoneFragment(Zone other) {
		super(other);
		mParents.put(other.getLayerName(), other);
		List<Zone> zones = new ArrayList<Zone>();
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
	private ZoneFragment[] splitAxis(Vector pos, Axis axis) {
		ZoneFragment[] result = (ZoneFragment[]) new ZoneFragment[2];

		ZoneFragment lower = new ZoneFragment(this);
		ZoneFragment upper = new ZoneFragment(this);

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
	protected List<ZoneFragment> splitByOverlap(ZoneBase overlap, Zone newParent) {
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
	protected List<ZoneFragment> splitByOverlap(ZoneBase overlap, Zone newParent, boolean includeOverlap) {
		ZoneFragment centerZone = new ZoneFragment(this);

		Vector otherMin = overlap.minCorner();
		Vector otherMax = overlap.maxCornerExclusive();

		ZoneFragment[] tempSplitResult;
		List<ZoneFragment> result = new ArrayList<ZoneFragment>();

		for (Axis axis : Axis.values()) {
			@Nullable ZoneFragment lower;
			@Nullable ZoneFragment upper;

			List<ZoneFragment> workZones = result;
			result = new ArrayList<ZoneFragment>();

			for (@Nullable ZoneFragment workZone : workZones) {
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
		@Nullable List<Zone> newParentLayerZones = centerZone.mParentsAndEclipsed.get(newParentLayer);
		if (newParentLayerZones == null) {
			newParentLayerZones = new ArrayList<Zone>();
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
	 * Returns the merged ZoneFragment or null.
	 */
	protected @Nullable ZoneFragment merge(ZoneFragment other) {
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
			return new ZoneFragment(this);
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
		result.maxCornerExclusive(resultMax);

		return result;
	}

	public Map<String, Zone> getParents() {
		Map<String, Zone> result = new HashMap<String, Zone>();
		result.putAll(mParents);
		return result;
	}

	public Zone getParent(String layer) {
		return mParents.get(layer);
	}

	public Map<String, List<Zone>> getParentsAndEclipsed() {
		Map<String, List<Zone>> result = new HashMap<String, List<Zone>>();
		for (Map.Entry<String, List<Zone>> entry : mParentsAndEclipsed.entrySet()) {
			String layerName = entry.getKey();
			List<Zone> zones = entry.getValue();

			List<Zone> resultZones = new ArrayList<Zone>();
			resultZones.addAll(zones);
			result.put(layerName, resultZones);
		}
		return result;
	}

	public List<Zone> getParentAndEclipsed(String layer) {
		@Nullable List<Zone> zones = mParentsAndEclipsed.get(layer);
		List<Zone> result = new ArrayList<Zone>();
		if (zones != null) {
			result.addAll(zones);
		}
		return result;
	}

	public boolean hasProperty(String layerName, String propertyName) {
		@Nullable Zone zone = getParent(layerName);
		return zone != null && zone.hasProperty(propertyName);
	}

	/*
	 * Force all future tests for locations within this zone to return false.
	 *
	 * This means any code tracking previous fragments/zones will be forced to check again when reloading zones.
	 */
	protected void invalidate() {
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
			double test = VectorUtils.vectorAxis(loc, axis);
			double min = VectorUtils.vectorAxis(minCorner(), axis);
			double max = VectorUtils.vectorAxis(maxCornerExclusive(), axis);
			if (test < min || test >= max) {
				return false;
			}
		}
		return true;
	}

	public boolean equals(ZoneFragment other) {
		return (super.equals(other) &&
		        mParents.equals(other.mParents) &&
		        mParentsAndEclipsed.equals(other.mParentsAndEclipsed));
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(Object o) {
		if (!(o instanceof ZoneFragment)) {
			return false;
		}
		ZoneFragment other = (ZoneFragment)o;
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
