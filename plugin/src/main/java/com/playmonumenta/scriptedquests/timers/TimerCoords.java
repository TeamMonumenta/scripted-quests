package com.playmonumenta.scriptedquests.timers;

public class TimerCoords implements Comparable<TimerCoords> {
	private final String mWorldName;
	private final int mX;
	private final int mY;
	private final int mZ;

	public TimerCoords(String worldName, int x, int y, int z) {
		mWorldName = worldName;
		mX = x;
		mY = y;
		mZ = z;
	}

	@Override
	public int compareTo(TimerCoords c) {
		int ret;

		ret = Integer.compare(mX, c.mX);
		if (ret == 0) {
			ret = Integer.compare(mZ, c.mZ);
		}
		if (ret == 0) {
			ret = Integer.compare(mY, c.mY);
		}
		if (ret == 0) {
			ret = mWorldName.compareTo(c.mWorldName);
		}

		return ret;
	}
}

