package com.playmonumenta.scriptedquests.zones;

import com.playmonumenta.scriptedquests.utils.VectorUtils;

import org.bukkit.Axis;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ZoneBase {
	protected Vector mPosition;
	protected Vector mSize;

	public ZoneBase(ZoneBase other) {
		mPosition = other.mPosition.clone();
		mSize = other.mSize.clone();
	}

	/*
	 * pos1 and pos2 are used similar to /fill:
	 * - Both are inclusive coordinates.
	 * - The minimum/maximum are determined for you.
	 */
	public ZoneBase(Vector pos1, Vector pos2) {
		Vector posMin = Vector.getMinimum(pos1, pos2);
		Vector posMax = Vector.getMaximum(pos1, pos2);

		mPosition = posMin;
		mSize = posMax.clone();
		mSize.subtract(posMin);
		mSize.add(new Vector(1, 1, 1));
	}

	/*
	 * Returns the smallest integer coordinate that is inside the zone on each axis.
	 */
	public Vector minCorner() {
		return mPosition.clone();
	}

	/*
	 * Sets the smallest integer coordinate that is inside the zone on each axis.
	 */
	public void minCorner(Vector other) {
		Vector newPosition = mPosition.clone();
		Vector newSize = mSize.clone();

		newSize.add(mPosition);
		newSize.subtract(other);
		newPosition = other.clone();

		mPosition = newPosition;
		mSize = newSize;
	}

	/*
	 * Returns the largest integer coordinate that is inside the zone on each axis.
	 */
	public Vector maxCorner() {
		Vector result = maxCornerExclusive();
		result.subtract(new Vector(1, 1, 1));
		return result;
	}

	/*
	 * Sets the largest integer coordinate that is inside the zone on each axis.
	 */
	public void maxCorner(Vector other) {
		Vector newSize = mSize.clone();

		newSize.add(other);
		newSize.subtract(maxCorner());

		mSize = newSize;
	}

	/*
	 * Returns the smallest coordinate that is outside the zone on each axis.
	 */
	public Vector maxCornerExclusive() {
		Vector result = mPosition.clone();
		result.add(mSize);
		return result;
	}

	/*
	 * Sets the smallest coordinate that is outside the zone on each axis.
	 */
	public void maxCornerExclusive(Vector other) {
		Vector newSize = mSize.clone();

		newSize.add(other);
		newSize.subtract(maxCornerExclusive());

		mSize = newSize;
	}

	public BoundingBox boundingBox() {
		return BoundingBox.of(minCorner(), maxCornerExclusive());
	}

	public Vector size() {
		return mSize.clone();
	}

	public double volume() {
		return mSize.getX() * mSize.getY() * mSize.getZ();
	}

	/*
	 * True if the size on each axis is > 0, else false.
	 */
	public boolean isValid() {
		return mSize.getX() > 0 && mSize.getY() > 0 && mSize.getZ() > 0;
	}

	public boolean within(Vector loc) {
		if (loc == null) {
			return false;
		}

		Vector min = minCorner();
		Vector max = maxCornerExclusive();

		return (min.getX() <= loc.getX() &&
		        loc.getX() < max.getX() &&
		        min.getY() <= loc.getY() &&
		        loc.getY() < max.getY() &&
		        min.getZ() <= loc.getZ() &&
		        loc.getZ() < max.getZ());
	}

	/*
	 * Returns a ZoneBase that is inside this and the other zone.
	 */
	public @Nullable ZoneBase overlappingZone(ZoneBase other) {
		Vector selfMin = minCorner();
		Vector selfMax = maxCorner();
		Vector otherMin = other.minCorner();
		Vector otherMax = other.maxCorner();

		for (Axis axis : Axis.values()) {
			if (VectorUtils.vectorAxis(selfMax, axis) < VectorUtils.vectorAxis(otherMin, axis) ||
				VectorUtils.vectorAxis(otherMax, axis) < VectorUtils.vectorAxis(selfMin, axis)) {
				return null;
			}
		}

		Vector resultMin = Vector.getMaximum(selfMin, otherMin);
		Vector resultMax = Vector.getMinimum(selfMax, otherMax);

		ZoneBase result = new ZoneBase(resultMin, resultMax);

		if (result.isValid()) {
			return result;
		}

		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ZoneBase)) {
			return false;
		}

		ZoneBase other = (ZoneBase)o;
		return (mPosition.equals(other.mPosition) &&
		        mSize.equals(other.mSize));
	}

	@Override
	public int hashCode() {
		return 31*mPosition.hashCode() + mSize.hashCode();
	}

	public String toString() {
		return "ZoneBase(" + minCorner().toString() + ", " + maxCorner().toString() + ")";
	}
}
