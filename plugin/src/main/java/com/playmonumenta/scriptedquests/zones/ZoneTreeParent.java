package com.playmonumenta.scriptedquests.zones;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.playmonumenta.scriptedquests.utils.VectorUtils;

import org.bukkit.Axis;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.dynmap.markers.MarkerSet;

public class ZoneTreeParent extends ZoneTreeBase {
	// The axis this node is split over.
	private Axis mAxis;
	// The pivot for mMore/mLess
	private double mPivot;
	// Lowest/highest value for this node and its children; allows returning null early
	private double mMin;
	private double mMax;
	// Branch that is Less/More than pivot
	private ZoneTreeBase mLess;
	private ZoneTreeBase mMore;

	// Some zones may overlap the pivot
	// Min coordinate of middle zones
	private double mMidMin;
	// Max coordinate of middle zones
	private double mMidMax;
	// Branch that contains the pivot
	private ZoneTreeBase mMid;

	private static final Axis[] AXIS_ORDER = {Axis.X, Axis.Z, Axis.Y};

	public ZoneTreeParent(List<ZoneFragment> zones) throws Exception {
		/*
		 * Local class is used to get best balance without
		 * exposing incomplete results or creating tree nodes.
		 */

		 class ParentData {
			public int mPriority;
			public Axis mAxis;
			public double mPivot;
			public double mMidMin;
			public double mMidMax;
			public List<ZoneFragment> mLess = new ArrayList<ZoneFragment>();
			public List<ZoneFragment> mMid = new ArrayList<ZoneFragment>();
			public List<ZoneFragment> mMore = new ArrayList<ZoneFragment>();
		}

		mFragmentCount = zones.size();

		Vector minVector = zones.get(0).minCorner();
		Vector maxVector = zones.get(0).maxCornerExclusive();

		// Default is an impossibly worst case scenario so it will never be chosen.
		ParentData bestSplit = new ParentData();
		bestSplit.mPriority = mFragmentCount;

		for (ZoneFragment pivotZone : zones) {
			minVector = Vector.getMinimum(minVector, pivotZone.minCorner());
			maxVector = Vector.getMaximum(maxVector, pivotZone.maxCornerExclusive());

			for (Axis axis : AXIS_ORDER) {
				double[] possiblePivots = new double[2];
				possiblePivots[0] = VectorUtils.vectorAxis(pivotZone.minCorner(), axis);
				possiblePivots[1] = VectorUtils.vectorAxis(pivotZone.maxCornerExclusive(), axis);
				for (double pivot : possiblePivots) {
					ParentData testSplit = new ParentData();
					testSplit.mAxis = axis;
					testSplit.mPivot = pivot;
					testSplit.mMidMin = pivot;
					testSplit.mMidMax = pivot;

					for (ZoneFragment zone : zones) {
						if (pivot >= VectorUtils.vectorAxis(zone.maxCornerExclusive(), axis)) {
							testSplit.mLess.add(zone);
						} else if (pivot >= VectorUtils.vectorAxis(zone.minCorner(), axis)) {
							testSplit.mMidMin = Math.min(testSplit.mMidMin,
							                             VectorUtils.vectorAxis(zone.minCorner(), axis));
							testSplit.mMidMax = Math.max(testSplit.mMidMax,
							                             VectorUtils.vectorAxis(zone.maxCornerExclusive(), axis));
							testSplit.mMid.add(zone);
						} else {
							testSplit.mMore.add(zone);
						}
					}

					testSplit.mPriority = Math.max(testSplit.mLess.size(), testSplit.mMore.size());
					testSplit.mPriority = Math.max(testSplit.mPriority, testSplit.mMid.size());

					if (testSplit.mPriority < bestSplit.mPriority) {
						bestSplit = testSplit;
					}
				}
			}
		}

		// This is the answer we want. Copy values to self.
		mAxis = bestSplit.mAxis;
		mMin = VectorUtils.vectorAxis(minVector, mAxis);
		mMax = VectorUtils.vectorAxis(maxVector, mAxis);
		mPivot = bestSplit.mPivot;
		mMidMin = bestSplit.mMidMin;
		mMidMax = bestSplit.mMidMax;

		if (bestSplit.mPriority >= mFragmentCount) {
			/*
			 * The priority of our best case scenario is equal to or worse than our worst case.
			 *
			 * This should only occur if two zone fragments overlap, which is prevented by earlier
			 * sections of code. If this occurs, then somehow zone fragments are not being divided
			 * or handled properly.
			 */
			StringBuilder message = new StringBuilder("A serious plugin error has occured. Zones involved:");
			for (ZoneFragment zone : zones) {
				message.append("\n- " + zone.toString());
			}
			throw new Exception(message.toString());
		} else {
			mLess = createZoneTree(bestSplit.mLess);
			mMid = createZoneTree(bestSplit.mMid);
			mMore = createZoneTree(bestSplit.mMore);
		}
	}

	protected void invalidate() {
		mLess.invalidate();
		mMid.invalidate();
		mMore.invalidate();
	}

	public Set<ZoneFragment> getZoneFragments(BoundingBox bb) {
		Set<ZoneFragment> result = new HashSet<>();
		double bbMin = VectorUtils.vectorAxis(bb.getMin(), mAxis);
		double bbMax = VectorUtils.vectorAxis(bb.getMax(), mAxis);
		if (bbMin < mPivot && bbMax > mMin) {
			result.addAll(mLess.getZoneFragments(bb));
		}
		if (bbMin < mMidMax && bbMax > mMidMin) {
			result.addAll(mMid.getZoneFragments(bb));
		}
		if (bbMin < mMax && bbMax > mPivot) {
			result.addAll(mMore.getZoneFragments(bb));
		}
		return result;
	}

	public @Nullable ZoneFragment getZoneFragment(Vector loc) {
		if (loc == null) {
			return null;
		}

		@Nullable ZoneFragment result = null;
		double test = VectorUtils.vectorAxis(loc, mAxis);

		// If the test point is outside this node, return null immediately.
		if (test < mMin || test >= mMax) {
			return null;
		}

		// Check zones that don't overlap the pivot first
		if (test > mPivot) {
			result = mMore.getZoneFragment(loc);
		} else {
			result = mLess.getZoneFragment(loc);
		}

		// If a result is not found, check zones that overlap the pivot
		if (result == null && mMidMin <= test && test < mMidMax) {
			result = mMid.getZoneFragment(loc);
		}

		return result;
	}

	public int maxDepth() {
		return 1 + Math.max(mLess.maxDepth(),
		                    Math.max(mMid.maxDepth(),
		                             mMore.maxDepth()));
	}

	protected int totalDepth() {
		int result = fragmentCount();
		result += mLess.totalDepth();
		result += mMid.totalDepth();
		result += mMore.totalDepth();
		return result;
	}

	protected void refreshDynmapTree(MarkerSet markerSet, int parentR, int parentG, int parentB) {
		mLess.refreshDynmapTree(markerSet,
		                        (parentR + 255)/2,
		                        parentG/2,
		                        parentB/2);
		mMid.refreshDynmapTree(markerSet,
		                       parentR/2,
		                       (parentG + 255)/2,
		                       parentB/2);
		mMore.refreshDynmapTree(markerSet,
		                        parentR/2,
		                        parentG/2,
		                        (parentB + 255)/2);
	}

	public String toString() {
		return ("(ZoneTreeParent(<List<ZoneFragment>>): "
		        + "mAxis=" + mAxis.toString() + ", "
		        + "mPivot=" + Double.toString(mPivot) + ", "
		        + "mMin=" + Double.toString(mMin) + ", "
		        + "mMax=" + Double.toString(mMax) + ", "
		        + "mLess=<ZoneTreeBase>, "
		        + "mMore=<ZoneTreeBase>, "
		        + "mMidMin=" + Double.toString(mMidMin) + ", "
		        + "mMidMax=" + Double.toString(mMidMax) + ", "
		        + "mMid=<ZoneTreeBase>)");
	}
}
