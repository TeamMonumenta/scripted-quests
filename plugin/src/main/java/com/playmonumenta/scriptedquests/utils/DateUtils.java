package com.playmonumenta.scriptedquests.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class DateUtils {
	//TODO Read from config, default to system time zone aka ZoneId.systemDefault() - Tim

	// Offset server time to UTC-9 to change when the new day arrives.
	// getDaysSinceEpoch() uses its own perceived epoch
	// so it should sync up nicely with changes in getDayOfWeek().
	// With this, quest stuff should match play's daily resets & daily reboots at 9am UTC
	public static ZoneId TIMEZONE = ZoneOffset.of("-9");

	public static int getYear() {
		return LocalDateTime.now(TIMEZONE).getYear();
	}

	public static int getMonth() {
		return LocalDateTime.now(TIMEZONE).getMonthValue();
	}

	public static int getDayOfMonth() {
		return LocalDateTime.now(TIMEZONE).getDayOfMonth();
	}

	// 1 is Sunday, 7 is Saturday
	public static int getDayOfWeek() {
		// .getValue() gives 1 for Monday, 7 for Sunday, so we cycle the numbers
		return LocalDateTime.now(TIMEZONE).getDayOfWeek().getValue() % 7 + 1;
	}

	public static long getDaysSinceEpoch() {
		// In our specified timezone, how many days we perceive it is since our 1 Jan 1970.
		// Different timezones have different dates for the same point in time,
		// so this simple comparison will yield different numbers of days for them
		return LocalDate.now(TIMEZONE).toEpochDay();
	}

	public static long getSecondsSinceEpoch() {
		// Note: This method is intentionally UTC-only.
		return LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC);
	}

	public static int getAmPm() {
		return getHourOfDay() >= 12 ? 1 : 0;
	}

	public static int getHourOfDay() {
		return LocalDateTime.now(TIMEZONE).getHour();
	}

	public static int getHourOfTwelve() {
		int hourOfTwelve = getHourOfDay() % 12;
		return (hourOfTwelve == 0) ? 12 : hourOfTwelve;
	}

	public static int getMinute() {
		return LocalDateTime.now(TIMEZONE).getMinute();
	}

	public static int getSecond() {
		return LocalDateTime.now(TIMEZONE).getSecond();
	}

	@SuppressWarnings("JavaLocalDateTimeGetNano")
	public static int getMs() {
		return LocalDateTime.now(TIMEZONE).getNano() / 1000000;
	}
}
