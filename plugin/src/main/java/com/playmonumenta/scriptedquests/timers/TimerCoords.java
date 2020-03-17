package com.playmonumenta.scriptedquests.timers;

public class TimerCoords implements Comparable<TimerCoords> {
	private final int mX;
	private final int mY;
	private final int mZ;

	public TimerCoords(int x, int y, int z) {
		mX = x;
		mY = y;
		mZ = z;
	}

	public int compareTo(TimerCoords c) {
		int ret;

		ret = mX - c.mX;
		if (ret == 0) {
			ret = mZ - c.mZ;
		}
		if (ret == 0) {
			ret = mY - c.mY;
		}

		return ret;
	}
}

