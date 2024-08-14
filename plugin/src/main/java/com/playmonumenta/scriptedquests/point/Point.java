package com.playmonumenta.scriptedquests.point;

import org.bukkit.Location;
import org.bukkit.World;

public class Point {
	public double mX;
	public double mY;
	public double mZ;
	public float mYaw;
	public float mPitch;

	public Point(double x, double y, double z) {
		this(x, y, z, 0, 0);
	}

	public Point(double x, double y, double z, float yaw, float pitch) {
		mX = x;
		mY = y;
		mZ = z;
		mYaw = yaw;
		mPitch = pitch;
	}

	public Point(Location loc) {
		mX = loc.getX();
		mY = loc.getY();
		mZ = loc.getZ();
		mYaw = loc.getYaw();
		mPitch = loc.getPitch();
	}

	@Override
	public String toString() {
		return "(" + mX + ", " + mY + ", " + mZ + ")";
	}

	public Location toLocation(World world) {
		return new Location(world, mX, mY, mZ, (float) mYaw, (float) mPitch);
	}
}
