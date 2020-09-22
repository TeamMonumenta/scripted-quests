package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;
import java.util.SplittableRandom;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;

public class RandomNumber {
	private static final SplittableRandom mRandom = new SplittableRandom();

	public static void register() {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("min", new IntegerArgument());
		arguments.put("max", new IntegerArgument());

		new CommandAPICommand("randomnumber")
			.withPermission(CommandPermission.fromString("scriptedquests.randomnumber"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				return mRandom.nextInt((Integer)args[0], (Integer)args[1] + 1);
			})
			.register();
	}
}
