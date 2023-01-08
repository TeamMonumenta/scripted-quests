package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.utils.DateUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.TextArgument;

public class GetDate {
	private static final ArgumentSuggestions SUGGESTIONS_FIELDS
		= ArgumentSuggestions.strings("Year", "Month", "DayOfMonth", "DayOfWeek", "DaysSinceEpoch",
		"SecondsSinceEpoch", "IsPm", "HourOfDay", "HourOfTwelve", "Minute", "Second", "Ms");

	public static void register() {
		new CommandAPICommand("getdate")
			.withPermission(CommandPermission.fromString("scriptedquests.getdate"))
			.withArguments(new TextArgument("field").replaceSuggestions(SUGGESTIONS_FIELDS))
			.executes((sender, args) -> {
					return getField((String) args[0]);
				})
			.register();
	}

	private static int getField(String field) {
		return switch (field) {
			case "Year" -> DateUtils.getYear();
			case "Month" -> DateUtils.getMonth();
			case "DayOfMonth" -> DateUtils.getDayOfMonth();
			case "DayOfWeek" -> DateUtils.getDayOfWeek();
			case "DaysSinceEpoch" -> (int) DateUtils.getDaysSinceEpoch();
			case "SecondsSinceEpoch" -> (int) DateUtils.getSecondsSinceEpoch();
			case "IsPm" -> DateUtils.getAmPm();
			case "HourOfDay" -> DateUtils.getHourOfDay();
			case "HourOfTwelve" -> DateUtils.getHourOfTwelve();
			case "Minute" -> DateUtils.getMinute();
			case "Second" -> DateUtils.getSecond();
			case "Ms" -> DateUtils.getMs();
			default -> -1;
		};
	}
}
