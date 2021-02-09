package com.playmonumenta.scriptedquests.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

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
		return LocalDateTime.now(mTz).getDayOfWeek().getValue() % 7 + 1;
	}

	public static long getDaysSinceEpoch() {
		return ChronoUnit.DAYS.between(LocalDate.ofEpochDay(0), LocalDateTime.now(mTz));
	}

	public static long getSecondsSinceEpoch() {
		return ChronoUnit.SECONDS.between(LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC), LocalDateTime.now(ZoneOffset.UTC));
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
