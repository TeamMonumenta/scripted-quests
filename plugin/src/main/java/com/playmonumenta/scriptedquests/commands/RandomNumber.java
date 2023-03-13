package com.playmonumenta.scriptedquests.commands;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.IntegerArgument;
import java.util.SplittableRandom;

public class RandomNumber {
	private static final SplittableRandom mRandom = new SplittableRandom();

	public static void register() {
		new CommandAPICommand("randomnumber")
			.withPermission(CommandPermission.fromString("scriptedquests.randomnumber"))
			.withArguments(new IntegerArgument("min"))
			.withArguments(new IntegerArgument("max"))
			.executes((sender, args) -> {
				int min = (Integer)args[0];
				int max = (Integer)args[1];
				if (max < min) {
					throw CommandAPI.failWithString("max < min");
				}
				return mRandom.nextInt(min, max + 1);
			})
			.register();
	}
}
