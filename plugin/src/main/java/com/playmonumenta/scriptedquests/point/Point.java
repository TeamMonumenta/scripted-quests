package com.playmonumenta.scriptedquests.point;

import org.bukkit.Location;
import org.bukkit.World;

public class Point {
	public double mX;
	public double mY;
	public double mZ;

	public Point(double x, double y, double z) {
		mX = x;
		mY = y;
		mZ = z;
	}

	public Point(Location loc) {
		mX = loc.getX();
		mY = loc.getY();
		mZ = loc.getZ();
	}

	@Override
	public String toString() {
		return "(" + Double.toString(mX) + ", " +
			Double.toString(mY) + ", " + Double.toString(mZ) + ")";
	}

	public Location toLocation(World world) {
		return new Location(world, mX, mY, mZ);
	}
}
