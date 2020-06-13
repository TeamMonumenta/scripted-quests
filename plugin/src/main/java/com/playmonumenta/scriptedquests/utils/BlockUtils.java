package com.playmonumenta.scriptedquests.utils;

import java.util.EnumMap;
import java.util.Map;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;

public class BlockUtils {
	public static int drawLine(Location start, Location end, Material mat) {
		// https://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
		if (start == null || end == null || mat == null) {
			return 0;
		}

		int blocksChanged = 0;

		// Pre-calculate required values
		Map<Axis, Double> deltas = new EnumMap<>(Axis.class);
		Map<Axis, Double> deltaSizes = new EnumMap<>(Axis.class);
		Map<Axis, Double> deltaSigns = new EnumMap<>(Axis.class);
		for (Axis axis : Axis.values()) {
			double deltaAxis = VectorUtils.vectorAxis(end, axis) - VectorUtils.vectorAxis(start, axis);
			double deltaAxisSize = Math.abs(deltaAxis);
			deltas.put(axis, deltaAxis);
			deltaSizes.put(axis, deltaAxisSize);
			deltaSigns.put(axis, deltaAxis < 0.0 ? -1.0 : 1.0);
		}

		// Axes from largest to smallest delta
		Axis[] axisOrder = Axis.values();
		if (deltaSizes.get(axisOrder[0]) < deltaSizes.get(axisOrder[1])) {
			Axis axisSwap = axisOrder[0];
			axisOrder[0] = axisOrder[1];
			axisOrder[1] = axisSwap;
		}
		if (deltaSizes.get(axisOrder[1]) < deltaSizes.get(axisOrder[2])) {
			Axis axisSwap = axisOrder[1];
			axisOrder[1] = axisOrder[2];
			axisOrder[2] = axisSwap;
		}
		if (deltaSizes.get(axisOrder[0]) < deltaSizes.get(axisOrder[1])) {
			Axis axisSwap = axisOrder[0];
			axisOrder[0] = axisOrder[1];
			axisOrder[1] = axisSwap;
		}

		// Swap start and end if largest delta axis is negative
		if (deltas.get(axisOrder[0]) < 0.0) {
			Location swapLocations = start;
			start = end;
			end = swapLocations;

			for (Axis axis : Axis.values()) {
				deltas.put(axis, -deltas.get(axis));
				deltaSigns.put(axis, -deltaSigns.get(axis));
			}
		}

		Location workLoc = start.clone();

		double majorPos = VectorUtils.vectorAxis(start, axisOrder[0]);
		double majorEnd = VectorUtils.vectorAxis(end, axisOrder[0]);

		// Axis-aligned check; skip needless computation
		if (deltas.get(axisOrder[1]) == 0.0 && deltas.get(axisOrder[2]) == 0.0) {
			while (majorPos <= majorEnd) {
				workLoc.getBlock().setType(mat);
				++blocksChanged;

				workLoc = workLoc.clone();
				majorPos = VectorUtils.vectorAxisIncrement(workLoc, axisOrder[0]);
			}
			return blocksChanged;
		}

		// Pre-compute values needed if not axis-aligned
		// When error > 0.0, increment that axis
		double midError = 2 * deltaSizes.get(axisOrder[1]) - deltaSizes.get(axisOrder[0]);
		double minorError = 2 * deltaSizes.get(axisOrder[2]) - deltaSizes.get(axisOrder[0]);
		// Amount to increment error when major axis increments and error <= 0.0
		double midErrorDeltaStraight = 2 * deltaSizes.get(axisOrder[1]);
		double minorErrorDeltaStraight = 2 * deltaSizes.get(axisOrder[2]);
		// Amount to increment error when major axis increments and error > 0.0
		double midErrorDeltaDiagonal = 2 * (deltaSizes.get(axisOrder[1]) - deltaSizes.get(axisOrder[0]));
		double minorErrorDeltaDiagonal = 2 * (deltaSizes.get(axisOrder[2]) - deltaSizes.get(axisOrder[0]));

		// Loop over major axis
		while (majorPos <= majorEnd) {
			// Change the currently selected block
			workLoc.getBlock().setType(mat);
			++blocksChanged;

			// Increment major axis
			workLoc = workLoc.clone();
			majorPos = VectorUtils.vectorAxisIncrement(workLoc, axisOrder[0]);

			// Adjust error for mid axis and increment as needed
			if (midError <= 0.0) {
				// Go straight
				midError += midErrorDeltaStraight;
			} else {
				// Increment on axis
				midError += midErrorDeltaDiagonal;
				VectorUtils.vectorAxisIncrement(workLoc, axisOrder[1], deltaSigns.get(axisOrder[1]));
			}

			// Adjust error for minor axis and increment as needed
			if (minorError <= 0.0) {
				// Go straight
				minorError += minorErrorDeltaStraight;
			} else {
				// Increment on axis
				minorError += minorErrorDeltaDiagonal;
				VectorUtils.vectorAxisIncrement(workLoc, axisOrder[2], deltaSigns.get(axisOrder[2]));
			}
		}

		return blocksChanged;
	}
}
