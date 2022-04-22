package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.utils.DateUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.TextArgument;

public class GetDate {
	private static final String[] FIELDS = new String[] {"Year", "Month", "DayOfMonth", "DayOfWeek", "DaysSinceEpoch", "SecondsSinceEpoch",
	                                                     "IsPm", "HourOfDay", "HourOfTwelve", "Minute", "Second", "Ms", };

	public static void register() {
		new CommandAPICommand("getdate")
			.withPermission(CommandPermission.fromString("scriptedquests.getdate"))
			.withArguments(new TextArgument("field").replaceSuggestions(info -> {
				return FIELDS;
			}))
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
