package pe.scriptedquests.point;

import org.bukkit.Location;

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

	public String toString() {
		return "(" + Double.toString(mX) + ", " +
			Double.toString(mY) + ", " + Double.toString(mZ) + ")";
	}
}
