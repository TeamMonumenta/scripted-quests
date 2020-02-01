package com.playmonumenta.scriptedquests.utils;

import java.awt.Color;

import org.bukkit.Axis;
import org.bukkit.util.Vector;

public class ZoneUtils {
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

	public static int getColor(String layerName, String zoneName) {
		int layerNameHash = layerName.hashCode();
		int zoneNameHash = zoneName.hashCode();

		// Start with HSV color space, which is similar to how the human eye works.

		// Magic numbers here are arbitrary, but >= 255. 0.0 <= hue < 1.0
		// Magic numbers can be tweaked for better visual effect.
		// Bitwise & used in place of modulus, as % is remainder (breaks on negatives!)
		// H
		float hue = (float) layerNameHash / 1023.0f + 0.31f; // Default doesn't *need* to be blue, but I *like* it blue.
		// Saturation and value (brightness) are biased away from gray.
		// S
		float saturation = 1.0f - 0.5f * ((float) (zoneNameHash & 511)) / 511.0f;
		// V
		float value = 1.0f - 0.5f * ((float) ((zoneNameHash / 511) & 511)) / 511.0f;

		// Mask out alpha channel, dynmap expects only RGB.
		return Color.HSBtoRGB(hue, saturation, value) & 0x00ffffff;
	}
}
