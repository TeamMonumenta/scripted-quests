package com.playmonumenta.scriptedquests.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class RaceUtils {
	public static List<Location> transformPoints(Location center, List<Vector> points, double yaw, double pitch, double roll, double scale) {
		// Convert to radians
		yaw = Math.toRadians(yaw);
		pitch = Math.toRadians(pitch);
		roll = Math.toRadians(roll);

		List<Location> list = new ArrayList<>();
		// Store the values so we don't have to calculate them again for every single point.
		double cp = Math.cos(pitch);
		double sp = Math.sin(pitch);
		double cy = Math.cos(yaw);
		double sy = Math.sin(yaw);
		double cr = Math.cos(roll);
		double sr = Math.sin(roll);
		double x;
		double bx;
		double y;
		double by;
		double z;
		double bz;

		for (Vector point : points) {
			x = point.getX();
			bx = x;
			y = point.getY();
			by = y;
			z = point.getZ();
			bz = z;
			x = ((x * cy - bz * sy) * cr + by * sr) * scale;
			y = ((y * cp + bz * sp) * cr - bx * sr) * scale;
			z = ((z * cp - by * sp) * cy + bx * sy) * scale;
			list.add(new Location(center.getWorld(), (center.getX() + x), (center.getY() + y), (center.getZ() + z)));
		}

		// list contains all the locations of the rotated shape at the specified center
		return list;
	}

	public static String msToTimeString(int ms) {
		//converts given milliseconds to a hh:mm:ss:lll string
		int hours = ms / 3600000;
		ms -= hours * 3600000;
		int minutes = ms / 60000;
		ms -= minutes * 60000;
		int seconds = ms / 1000;
		int milliseconds = (int)(ms - (seconds * 1000));
		String out = String.format("%s%s%s%s",
		                           hours == 0 ? "" : String.format("%d:", hours),
		                           (minutes == 0 && hours == 0) ? "" : String.format(((hours == 0) ? "%d:" : "%02d:"), minutes),
		                           String.format(((hours == 0 && minutes == 0) ? "%d:" : "%02d:"), seconds),
		                           String.format("%03d", milliseconds));
		return out;
	}
}
