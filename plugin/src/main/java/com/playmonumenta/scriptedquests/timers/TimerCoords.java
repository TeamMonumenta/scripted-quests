package com.playmonumenta.scriptedquests.timers;

public class TimerCoords implements Comparable<TimerCoords> {
	public final int x;
	public final int y;
	public final int z;

	public TimerCoords(int xi, int yi, int zi) {
		x = xi;
		y = yi;
		z = zi;
	}

	public int compareTo(TimerCoords c) {
		int ret;

		ret = x - c.x;
		if (ret == 0) {
			ret = z - c.z;
		}
		if (ret == 0) {
			ret = y - c.y;
		}

		return ret;
	}
}

