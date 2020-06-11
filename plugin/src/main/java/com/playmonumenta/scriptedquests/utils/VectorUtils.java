package com.playmonumenta.scriptedquests.utils;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class VectorUtils {
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

	public static double vectorAxis(Location loc, Axis axis) {
		switch (axis) {
		case X:
			return loc.getX();
		case Z:
			return loc.getZ();
		default:
			return loc.getY();
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

	public static void vectorAxis(Location loc, Axis axis, double value) {
		switch (axis) {
		case X:
			loc.setX(value);
			break;
		case Z:
			loc.setZ(value);
			break;
		default:
			loc.setY(value);
		}
	}

	public static double vectorAxisIncrement(Vector vector, Axis axis) {
		return vectorAxisIncrement(vector, axis, 1.0);
	}

	public static double vectorAxisIncrement(Location loc, Axis axis) {
		return vectorAxisIncrement(loc, axis, 1.0);
	}

	public static double vectorAxisIncrement(Vector vector, Axis axis, double value) {
		double newVal;
		switch (axis) {
		case X:
			newVal = vector.getX() + value;
			vector.setX(newVal);
			return newVal;
		case Z:
			newVal = vector.getZ() + value;
			vector.setZ(newVal);
			return newVal;
		default:
			newVal = vector.getY() + value;
			vector.setY(newVal);
			return newVal;
		}
	}

	public static double vectorAxisIncrement(Location loc, Axis axis, double value) {
		double newVal;
		switch (axis) {
		case X:
			newVal = loc.getX() + value;
			loc.setX(newVal);
			return newVal;
		case Z:
			newVal = loc.getZ() + value;
			loc.setZ(newVal);
			return newVal;
		default:
			newVal = loc.getY() + value;
			loc.setY(newVal);
			return newVal;
		}
	}
}
