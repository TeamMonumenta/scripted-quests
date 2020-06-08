package com.playmonumenta.scriptedquests.utils;

import java.util.Calendar;

public class DateUtils {
	public static final int TICKS_PER_SECOND = 20;
	public static final int SECONDS_PER_MINUTE = 60;
	public static final int TICKS_PER_MINUTE = SECONDS_PER_MINUTE * TICKS_PER_SECOND;
	public static final int MINUTES_PER_HOUR = 60;
	public static final int SECONDS_PER_HOUR = MINUTES_PER_HOUR * SECONDS_PER_MINUTE;
	public static final int TICKS_PER_HOUR = MINUTES_PER_HOUR * TICKS_PER_MINUTE;
	public static final int HOURS_PER_DAY = 24;
	public static final int MINUTES_PER_DAY = HOURS_PER_DAY * MINUTES_PER_HOUR;
	public static final int SECONDS_PER_DAY = HOURS_PER_DAY * SECONDS_PER_HOUR;
	public static final int TICKS_PER_DAY = HOURS_PER_DAY * TICKS_PER_HOUR;

	public static Calendar getCalendar() {
		return Calendar.getInstance();
	}

	public static Calendar getCalendarFromTimestamp(Long timestamp) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timestamp);
		return cal;
	}

	public static int getYear() {
		return Calendar.getInstance().get(Calendar.YEAR);
	}

	public static int getYear(Calendar cal) {
		return cal.get(Calendar.YEAR);
	}

	public static int getMonth() {
		return Calendar.getInstance().get(Calendar.MONTH) + 1;
	}

	public static int getMonth(Calendar cal) {
		return cal.get(Calendar.MONTH) + 1;
	}

	public static int getDayOfMonth() {
		return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
	}

	public static int getDayOfMonth(Calendar cal) {
		return cal.get(Calendar.DAY_OF_MONTH);
	}

	// 1 is Sunday, 7 is Saturday
	public static int getDayOfWeek() {
		return Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
	}

	public static int getDayOfWeek(Calendar cal) {
		return cal.get(Calendar.DAY_OF_WEEK);
	}

	public static boolean isDst() {
		return Calendar.getInstance().get(Calendar.DST_OFFSET) != 0;
	}

	public static int getAmPm() {
		return Calendar.getInstance().get(Calendar.AM_PM);
	}

	public static int getAmPm(Calendar cal) {
		return cal.get(Calendar.AM_PM);
	}

	public static int getHourOfDay() {
		return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
	}

	public static int getHourOfDay(Calendar cal) {
		return cal.get(Calendar.HOUR_OF_DAY);
	}

	public static int getHourOfTwelve() {
		return Calendar.getInstance().get(Calendar.HOUR);
	}

	public static int getHourOfTwelve(Calendar cal) {
		return cal.get(Calendar.HOUR);
	}

	public static int getMinute() {
		return Calendar.getInstance().get(Calendar.MINUTE);
	}

	public static int getMinute(Calendar cal) {
		return cal.get(Calendar.MINUTE);
	}

	public static int getSecond() {
		return Calendar.getInstance().get(Calendar.SECOND);
	}

	public static int getSecond(Calendar cal) {
		return cal.get(Calendar.SECOND);
	}

	public static int getMs() {
		return Calendar.getInstance().get(Calendar.MILLISECOND);
	}

	public static int getMs(Calendar cal) {
		return cal.get(Calendar.MILLISECOND);
	}

	public static long getTimestamp() {
		return Calendar.getInstance().getTimeInMillis();
	}

	public static String prettyDelta(int seconds) {
		if (seconds < 0) {
			return prettyDelta(-seconds);
		}

		if (seconds >= SECONDS_PER_DAY) {
			int hours = seconds / SECONDS_PER_HOUR;
			int days = hours / HOURS_PER_DAY;
			hours %= HOURS_PER_DAY;

			if (hours < 10) {
				return Integer.toString(days) + "d 0" + Integer.toString(hours) + "h";
			} else {
				return Integer.toString(days) + "d " + Integer.toString(hours) + "h";
			}
		} else if (seconds >= SECONDS_PER_HOUR) {
			int minutes = seconds / SECONDS_PER_MINUTE;
			int hours = minutes / MINUTES_PER_HOUR;
			minutes %= MINUTES_PER_HOUR;

			if (minutes < 10) {
				return Integer.toString(hours) + "h 0" + Integer.toString(minutes) + "m";
			} else {
				return Integer.toString(hours) + "h " + Integer.toString(minutes) + "m";
			}
		} else if (seconds >= SECONDS_PER_MINUTE) {
			int minutes = seconds / SECONDS_PER_MINUTE;
			seconds %= SECONDS_PER_MINUTE;

			if (seconds < 10) {
				return Integer.toString(minutes) + "m 0" + Integer.toString(seconds) + "s";
			} else {
				return Integer.toString(minutes) + "m " + Integer.toString(seconds) + "s";
			}
		} else {
			return Integer.toString(seconds);
		}
	}
}
