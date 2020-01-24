package com.playmonumenta.scriptedquests.utils;

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

		// Magic numbers here are arbitrary, but >= 255. 360.0 is degrees in a circle.
		// Magic numbers can be tweaked for better visual effect.
		// Bitwise & used in place of modulus, as % is remainder (breaks on negatives!)
		// H
		double hue = 360.0 * ((double) (layerNameHash & 511)) / 511.0;
		// Saturation and value (brightness) are biased away from gray.
		// S
		double saturation = 1.0 - 0.5 * ((double) (zoneNameHash & 511)) / 511.0;
		// V
		double value = 1.0 - 0.5 * ((double) ((zoneNameHash / 511) & 511)) / 511.0;

		return hsvToRgb(hue, saturation, value);
	}

	public static int hsvToRgb(double hue, double saturation, double value) {
		// https://www.rapidtables.com/convert/color/hsv-to-rgb.html

		// H: 0.0 <= Hue < 360.0
		// S: 0.0 <= Saturation <= 1.0
		// V: 0.0 <= Value <= 1.0

		// C
		double brightestRgbChannel = value * saturation;
		// X
		double middleRgbChannel = brightestRgbChannel * (1.0 - Math.abs((hue / 60.0) % 2.0 - 1.0));
		// m
		double offsetRgb = value - brightestRgbChannel;

		// Java initializes these to 0 for us.
		// R', G', B'
		double rgbPrimeArray[] = new double[3];
		// R, G, B
		int rgbArray[] = new int[3];

		int rgbOrderIndex = (int) (hue / 60.0);
		switch (rgbOrderIndex) {
		case 0:
			rgbPrimeArray[0] = brightestRgbChannel;
			rgbPrimeArray[1] = middleRgbChannel;
			rgbPrimeArray[2] = 0.0;
			break;
		case 1:
			rgbPrimeArray[0] = middleRgbChannel;
			rgbPrimeArray[1] = brightestRgbChannel;
			rgbPrimeArray[2] = 0.0;
			break;
		case 2:
			rgbPrimeArray[0] = 0.0;
			rgbPrimeArray[1] = brightestRgbChannel;
			rgbPrimeArray[2] = middleRgbChannel;
			break;
		case 3:
			rgbPrimeArray[0] = 0.0;
			rgbPrimeArray[1] = middleRgbChannel;
			rgbPrimeArray[2] = brightestRgbChannel;
			break;
		case 4:
			rgbPrimeArray[0] = middleRgbChannel;
			rgbPrimeArray[1] = 0.0;
			rgbPrimeArray[2] = brightestRgbChannel;
			break;
		default:
			rgbPrimeArray[0] = brightestRgbChannel;
			rgbPrimeArray[1] = 0.0;
			rgbPrimeArray[2] = middleRgbChannel;
		}

		int result = 0;
		for (int rgbIndex = 2; rgbIndex >= 0; rgbIndex--) {
			result <<= 8;
			rgbArray[rgbIndex] = (int) ((rgbPrimeArray[rgbIndex] + offsetRgb) * 255.0);
			result += rgbArray[rgbIndex];
		}

		return result;
	}
}
