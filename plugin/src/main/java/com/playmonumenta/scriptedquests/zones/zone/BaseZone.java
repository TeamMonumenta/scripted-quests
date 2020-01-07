package com.playmonumenta.scriptedquests.zones.zone;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class BaseZone implements Cloneable {
	protected Vector mPosition;
	protected Vector mSize;

	public BaseZone(BaseZone other) throws Exception {
		if (other == null) {
			throw new Exception("other may not be null.");
		}

		mPosition = other.mPosition.clone();
		mSize = other.mSize.clone();
	}

	/*
	 * pos1 and pos2 are used similar to /fill:
	 * - Both are inclusive coordinates.
	 * - The minimum/maximum are determined for you.
	 */
	public BaseZone(Vector pos1, Vector pos2) throws Exception {
		if (pos1 == null) {
			throw new Exception("pos1 may not be null.");
		}

		if (pos2 == null) {
			throw new Exception("pos2 may not be null.");
		}

		Vector posMin = Vector.getMinimum(pos1, pos2);
		Vector posMax = Vector.getMaximum(pos1, pos2);

		mPosition = posMin;
		mSize = posMax.clone();
		mSize.subtract(posMin);
		mSize.add(new Vector(1, 1, 1));
	}

	@Override
	public BaseZone clone() throws CloneNotSupportedException {
		try {
			return new BaseZone(this);
		} catch (CloneNotSupportedException e) {
			throw new Error(e);
		} catch (Exception e) {
			throw new CloneNotSupportedException("Cannot clone null object.");
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
	public void minCorner(Vector other) throws Exception {
		if (other == null) {
			throw new Exception("other may not be null.");
		}

		mSize.add(mPosition);
		mSize.subtract(other);
		mPosition = other.clone();
	}

	/*
	 * Returns the largest integer coordinate that is inside the zone on each axis.
	 */
	public Vector maxCorner() {
		Vector result = mPosition.clone();
		result.add(mSize);
		return result;
	}

	/*
	 * Sets the largest integer coordinate that is inside the zone on each axis.
	 */
	public void maxCorner(Vector other) throws Exception {
		if (other == null) {
			throw new Exception("other may not be null.");
		}

		mSize.add(other);
		mSize.subtract(mPosition);
	}

	/*
	 * Returns the smallest coordinate that is outside the zone on each axis.
	 */
	public Vector trueMaxCorner() {
		Vector result = maxCorner();
		result.add(new Vector(1, 1, 1));
		return result;
	}

	/*
	 * Sets the smallest coordinate that is outside the zone on each axis.
	 */
	public void trueMaxCorner(Vector other) throws Exception {
		if (other == null) {
			throw new Exception("other may not be null.");
		}

		maxCorner(other);
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

	public boolean within(Vector loc, Axis axis) {
		double test;
		double min;
		double max;

		switch (axis) {
		case X:
			test = loc.getX();
			min = minCorner().getX();
			max = trueMaxCorner().getX();
			break;
		case Z:
			test = loc.getZ();
			min = minCorner().getZ();
			max = trueMaxCorner().getZ();
			break;
		default:
			test = loc.getY();
			min = minCorner().getY();
			max = trueMaxCorner().getY();
		}

		return test >= min && test < max;
	}

	public boolean within(Vector loc) throws Exception {
		if (loc == null) {
			throw new Exception("loc may not be null.");
		}

		double x = loc.getX();
		double y = loc.getY();
		double z = loc.getZ();

		Vector min = minCorner();
		Vector max = trueMaxCorner();

		return x >= min.getX() && x < max.getX() && y >= min.getY() && y < max.getY() && z >= min.getZ() && z < max.getZ();
	}

	public boolean within(Location loc) throws Exception {
		if (loc == null) {
			throw new Exception("loc may not be null.");
		}

		return within(loc.toVector());
	}

	/*
	 * Returns a BaseZone that is inside this and the other zone.
	 */
	public BaseZone overlappingZone(BaseZone other) throws Exception {
		if (other == null) {
			throw new Exception("other may not be null.");
		}

		Vector selfMin = minCorner();
		Vector selfMax = maxCorner();
		Vector otherMin = other.minCorner();
		Vector otherMax = other.maxCorner();

		if (selfMax.getX() < otherMin.getX() ||
		    selfMax.getY() < otherMin.getY() ||
		    selfMax.getZ() < otherMin.getZ() ||
		    otherMax.getX() < selfMin.getX() ||
		    otherMax.getY() < selfMin.getY() ||
		    otherMax.getZ() < selfMin.getZ()) {
			return null;
		}

		Vector resultMin = Vector.getMaximum(selfMin, otherMin);
		Vector resultMax = Vector.getMinimum(selfMax, otherMax);

		return new BaseZone(resultMin, resultMax);
	}
}
