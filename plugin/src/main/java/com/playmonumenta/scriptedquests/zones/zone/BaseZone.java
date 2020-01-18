package com.playmonumenta.scriptedquests.zones.zone;

import org.bukkit.Axis;
import org.bukkit.util.Vector;

public class BaseZone {
	protected Vector mPosition;
	protected Vector mSize;

	public BaseZone(BaseZone other) {
		mPosition = other.mPosition.clone();
		mSize = other.mSize.clone();
	}

	/*
	 * pos1 and pos2 are used similar to /fill:
	 * - Both are inclusive coordinates.
	 * - The minimum/maximum are determined for you.
	 */
	public BaseZone(Vector pos1, Vector pos2) {
		Vector posMin = Vector.getMinimum(pos1, pos2);
		Vector posMax = Vector.getMaximum(pos1, pos2);

		mPosition = posMin;
		mSize = posMax.clone();
		mSize.subtract(posMin);
		mSize.add(new Vector(1, 1, 1));
	}

	public static double vectorAxis(Vector vector, Axis axis) {
		switch (axis) {
		case X:
			return vector.getX();
		case Z:
			return vector.getZ();
		default:
			return vector.getY();
		}
	}

	public static void vectorAxis(Vector vector, Axis axis, double value) {
		switch (axis) {
		case X:
			vector.setX(value);
			break;
		case Z:
			vector.setZ(value);
			break;
		default:
			vector.setY(value);
		}
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
		Vector result = trueMaxCorner();
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
	public Vector trueMaxCorner() {
		Vector result = mPosition.clone();
		result.add(mSize);
		return result;
	}

	/*
	 * Sets the smallest coordinate that is outside the zone on each axis.
	 */
	public void trueMaxCorner(Vector other) {
		Vector newSize = mSize.clone();

		newSize.add(other);
		newSize.subtract(trueMaxCorner());

		mSize = newSize;
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

		for (Axis axis : Axis.values()) {
			double test = vectorAxis(loc, axis);
			double min = vectorAxis(minCorner(), axis);
			double max = vectorAxis(trueMaxCorner(), axis);
			if (test < min || test >= max) {
				return false;
			}
		}
		return true;
	}

	/*
	 * Returns a BaseZone that is inside this and the other zone.
	 */
	public BaseZone overlappingZone(BaseZone other) {
		Vector selfMin = minCorner();
		Vector selfMax = maxCorner();
		Vector otherMin = other.minCorner();
		Vector otherMax = other.maxCorner();

		for (Axis axis : Axis.values()) {
			if (vectorAxis(selfMax, axis) < vectorAxis(otherMin, axis) ||
				vectorAxis(otherMax, axis) < vectorAxis(selfMin, axis)) {
				return null;
			}
		}

		Vector resultMin = Vector.getMaximum(selfMin, otherMin);
		Vector resultMax = Vector.getMinimum(selfMax, otherMax);

		BaseZone result = new BaseZone(resultMin, resultMax);

		if (result.isValid()) {
			return result;
		}

		return null;
	}

	public String toString() {
		return "BaseZone(" + minCorner().toString() + ", " + maxCorner().toString() + ")";
	}
}
