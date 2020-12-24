package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.scriptedquests.utils.DateUtils;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.TextArgument;

public class GetDate {
	private static final String[] FIELDS = new String[] {"Year", "Month", "DayOfMonth", "DayOfWeek", "DaysSinceEpoch", "SecondsSinceEpoch", "IsDst",
	                                                     "IsPm", "HourOfDay", "HourOfTwelve", "Minute", "Second", "Ms", };

	public static void register() {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("field", new TextArgument().overrideSuggestions(FIELDS));

		new CommandAPICommand("getdate")
			.withPermission(CommandPermission.fromString("scriptedquests.getdate"))
			.withArguments(arguments)
			.executes((sender, args) -> {
					return getField((String)args[0]);
				})
			.register();
	}

	private static int getField(String field) {
		switch (field) {
		case "Year":
			return DateUtils.getYear();
		case "Month":
			return DateUtils.getMonth();
		case "DayOfMonth":
			return DateUtils.getDayOfMonth();
		case "DayOfWeek":
			return DateUtils.getDayOfWeek();
		case "DaysSinceEpoch":
			return (int)DateUtils.getDaysSinceEpoch();
		case "SecondsSinceEpoch":
			return (int)DateUtils.getSecondsSinceEpoch();
		case "IsDst":
			return DateUtils.isDst() ? 1 : 0;
		case "IsPm":
			return DateUtils.getAmPm();
		case "HourOfDay":
			return DateUtils.getHourOfDay();
		case "HourOfTwelve":
			return DateUtils.getHourOfTwelve();
		case "Minute":
			return DateUtils.getMinute();
		case "Second":
			return DateUtils.getSecond();
		case "Ms":
			return DateUtils.getMs();
		default:
			return -1;
		}
	}
}
