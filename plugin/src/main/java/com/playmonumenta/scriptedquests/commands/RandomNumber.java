package com.playmonumenta.scriptedquests.commands;

import java.util.SplittableRandom;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.IntegerArgument;

public class RandomNumber {
	private static final SplittableRandom mRandom = new SplittableRandom();

	public static void register() {
		new CommandAPICommand("randomnumber")
			.withPermission(CommandPermission.fromString("scriptedquests.randomnumber"))
			.withArguments(new IntegerArgument("min"))
			.withArguments(new IntegerArgument("max"))
			.executes((sender, args) -> {
				return mRandom.nextInt((Integer)args[0], (Integer)args[1] + 1);
			})
			.register();
	}
}
