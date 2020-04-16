package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.scriptedquests.utils.DateUtils;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.TextArgument;

public class GetDate {
	private static final String[] FIELDS = new String[] {"Year", "Month", "DayOfMonth", "DayOfWeek", "IsDst"};

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
		if (field.equals("Year")) {
			return DateUtils.getYear();
		} else if (field.equals("Month")) {
			return DateUtils.getMonth();
		} else if (field.equals("DayOfMonth")) {
			return DateUtils.getDayOfMonth();
		} else if (field.equals("DayOfWeek")) {
			return DateUtils.getDayOfWeek();
		} else if (field.equals("IsDst")) {
			return DateUtils.isDst() ? 1 : 0;
		} else {
			return -1;
		}
	}
}
