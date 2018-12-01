package com.playmonumenta.scriptedquests.commands;

import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.IntegerArgument;
import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;

import java.util.LinkedHashMap;
import java.util.SplittableRandom;

public class RandomNumber {
	private static final SplittableRandom mRandom = new SplittableRandom();

	@SuppressWarnings("unchecked")
	public static void register() {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("min", new IntegerArgument());
		arguments.put("max", new IntegerArgument());

		CommandAPI.getInstance().register("randomnumber",
		                                  CommandPermission.fromString("scriptedquests.randomnumber"),
		                                  arguments,
		                                  (sender, args) -> {
											  return mRandom.nextInt((Integer)args[0], (Integer)args[1] + 1);
		                                  }
		);
	}
}
