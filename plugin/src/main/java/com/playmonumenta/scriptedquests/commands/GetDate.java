package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.scriptedquests.utils.DateUtils;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.TextArgument;

public class GetDate {
	private static final String[] FIELDS = new String[] {"Year", "Month", "DayOfMonth", "DayOfWeek", "IsDst",
	                                                     "IsPm", "HourOfDay", "HourOfTwelve", "Minute", "Second", "Ms"};

	public static void register() {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("field", new TextArgument().overrideSuggestions(FIELDS));

		CommandAPI.getInstance().register("getdate",
		                                  CommandPermission.fromString("scriptedquests.getdate"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      return getField((String)args[0]);
		                                  }
		);
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
