package com.playmonumenta.scriptedquests.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class DateUtils {
	// TODO Read from config, default to system time zone ala ZoneId.systemDefault()
	public static ZoneId mTz = ZoneId.of("America/New_York");

	public static int getYear() {
		return LocalDateTime.now(mTz).getYear();
	}

	public static int getMonth() {
		return LocalDateTime.now(mTz).getMonthValue();
	}

	public static int getDayOfMonth() {
		return LocalDateTime.now(mTz).getDayOfMonth();
	}

	// 1 is Sunday, 7 is Saturday
	public static int getDayOfWeek() {
		// .getValue() gives 1 for Monday, 7 for Sunday, so we cycle the numbers
		return LocalDateTime.now(mTz).getDayOfWeek().getValue() % 7 + 1;
	}

	public static long getDaysSinceEpoch() {
		// In our specified timezone, how many days we perceive it is since our 1 Jan 1970.
		// Different timezones have different dates for the same point in time,
		// so this simple comparison will yield different numbers of days for them
		return LocalDate.now(mTz).toEpochDay();
	}

	public static long getSecondsSinceEpoch() {
		// In our specified timezone, how many seconds we perceive it is since our 1 Jan 1970.
		// LocalDateTime "does not store or represent a time-zone",
		// "It cannot represent an instant on the time-line".
		// It does not actually compare to the fixed point in time, unix epoch.
		// Different timezones will yield different seconds for the same point in time
		return LocalDateTime.now(mTz).toEpochSecond(ZoneOffset.UTC); // No further offset applied to our number of seconds
	}

	public static int getAmPm() {
		return getHourOfDay() >= 12 ? 1 : 0;
	}

	public static int getHourOfDay() {
		return LocalDateTime.now(mTz).getHour();
	}

	public static int getHourOfTwelve() {
		int hourOfTwelve = getHourOfDay() % 12;
		return (hourOfTwelve == 0) ? 12 : hourOfTwelve;
	}

	public static int getMinute() {
		return LocalDateTime.now(mTz).getMinute();
	}

	public static int getSecond() {
		return LocalDateTime.now(mTz).getSecond();
	}

	public static int getMs() {
		return LocalDateTime.now(mTz).getNano() / 1000000;
	}
}
