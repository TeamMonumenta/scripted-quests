package com.playmonumenta.scriptedquests.utils;

import java.util.Calendar;

public class DateUtils {
	public static int getYear() {
		return Calendar.getInstance().get(Calendar.YEAR);
	}

	public static int getMonth() {
		return Calendar.getInstance().get(Calendar.MONTH) + 1;
	}

	public static int getDayOfMonth() {
		return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
	}

	// 1 is Sunday, 7 is Saturday
	public static int getDayOfWeek() {
		return Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
	}

	public static boolean isDst() {
		return Calendar.getInstance().get(Calendar.DST_OFFSET) != 0;
	}
}
