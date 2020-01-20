package com.playmonumenta.scriptedquests.zones.zonetree;

import java.lang.Math;
import java.util.ArrayList;

import org.bukkit.Axis;
import org.bukkit.util.Vector;

import com.playmonumenta.scriptedquests.zones.zone.BaseZone;
import com.playmonumenta.scriptedquests.zones.zone.ZoneFragment;

public class ParentZoneTree extends BaseZoneTree {
	// The axis this node is split over.
	private Axis mAxis;
	// The pivot for mMore/mLess
	private double mPivot;
	// Branch that is Less/More than pivot
	private BaseZoneTree mLess;
	private BaseZoneTree mMore;

	// Some zones may overlap the pivot
	// Min coordinate of middle zones
	private double mMidMin;
	// Max coordinate of middle zones
	private double mMidMax;
	// Branch that contains the pivot
	private BaseZoneTree mMid;

	private static final Axis[] AXIS_ORDER = {Axis.X, Axis.Z, Axis.Y};

	public ParentZoneTree(ArrayList<ZoneFragment> zones) {
		/*
		 * Local class is used to get best balance without
		 * exposinig incomplete results or creating tree nodes.
		 */

		 class ParentData {
			public int mPriority;
			public Axis mAxis;
			public double mPivot;
			public double mMidMin;
			public double mMidMax;
			public ArrayList<ZoneFragment> mLess = new ArrayList<ZoneFragment>();
			public ArrayList<ZoneFragment> mMid = new ArrayList<ZoneFragment>();
			public ArrayList<ZoneFragment> mMore = new ArrayList<ZoneFragment>();
		}

		// Default is an impossibly worst case scenario so it will never be chosen.
		ParentData bestSplit = new ParentData();
		bestSplit.mPriority = zones.size();

		for (ZoneFragment pivotZone : zones) {
			for (Axis axis : AXIS_ORDER) {
				double[] possiblePivots = new double[2];
				possiblePivots[0] = BaseZone.vectorAxis(pivotZone.minCorner(), axis);
				possiblePivots[1] = BaseZone.vectorAxis(pivotZone.trueMaxCorner(), axis);
				for (double pivot : possiblePivots) {
					ParentData testSplit = new ParentData();
					testSplit.mAxis = axis;
					testSplit.mPivot = pivot;
					testSplit.mMidMin = pivot;
					testSplit.mMidMax = pivot;

					for (ZoneFragment zone : zones) {
						if (pivot >= BaseZone.vectorAxis(zone.trueMaxCorner(), axis)) {
							testSplit.mLess.add(zone);
						} else if (pivot >= BaseZone.vectorAxis(zone.minCorner(), axis)) {
							testSplit.mMidMin = Math.min(testSplit.mMidMin,
							                             BaseZone.vectorAxis(zone.minCorner(), axis));
							testSplit.mMidMax = Math.max(testSplit.mMidMax,
							                             BaseZone.vectorAxis(zone.trueMaxCorner(), axis));
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
		mPivot = bestSplit.mPivot;
		mMidMin = bestSplit.mMidMin;
		mMidMax = bestSplit.mMidMax;

		if (bestSplit.mPriority == zones.size()) {
			// Debug information - I'm almost certain only the else case will occur past this commit,
			// but have left it in until this has been reviewed.
			System.out.format("A serious error has occured. Zone fragments dropped to keep us going.%n");
			System.out.format("Zones:%n");
			for (ZoneFragment zone : zones) {
				System.out.format("- " + zone.toString() + "%n");
			}
			ArrayList<ZoneFragment> tempDebugList = new ArrayList<ZoneFragment>();
			tempDebugList.add(zones.get(0));
			mLess = CreateZoneTree(tempDebugList);
			mMid = CreateZoneTree(tempDebugList);
			mMore = CreateZoneTree(tempDebugList);
		} else {
			mLess = CreateZoneTree(bestSplit.mLess);
			mMid = CreateZoneTree(bestSplit.mMid);
			mMore = CreateZoneTree(bestSplit.mMore);
		}
	}

	/*
	 * Invalidate all fragments in the tree, causing any players inside them to be
	 * considered outside them. This updates them to the correct zone automagically.
	 */
	public void invalidate() {
		mLess.invalidate();
		mMid.invalidate();
		mMore.invalidate();
	}

	public ZoneFragment getZoneFragment(Vector loc) {
		if (loc == null) {
			return null;
		}

		ZoneFragment result = null;
		double test = BaseZone.vectorAxis(loc, mAxis);

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

	public String toString() {
		return ("(ParentZoneTree(<ArrayList<ZoneFragment>>): "
		        + "mAxis=" + mAxis.toString() + ", "
		        + "mPivot=" + Double.toString(mPivot) + ", "
		        + "mLess=<BaseZoneTree>, "
		        + "mMore=<BaseZoneTree>, "
		        + "mMidMin=" + Double.toString(mMidMin) + ", "
		        + "mMidMax=" + Double.toString(mMidMax) + ", "
		        + "mMid=<BaseZoneTree>)");
	}
}
