package com.playmonumenta.scriptedquests.commands;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.IntegerArgument;
import java.util.SplittableRandom;

public class RandomNumber {
	private static final SplittableRandom mRandom = new SplittableRandom();

	public static void register() {
		IntegerArgument minArg = new IntegerArgument("min");
		IntegerArgument maxArg = new IntegerArgument("max");

		new CommandAPICommand("randomnumber")
			.withPermission(CommandPermission.fromString("scriptedquests.randomnumber"))
			.withArguments(minArg)
			.withArguments(maxArg)
			.executes((sender, args) -> {
				int min = args.getByArgument(minArg);
				int max = args.getByArgument(maxArg);
				if (max < min) {
					throw CommandAPI.failWithString("max < min");
				}
				return mRandom.nextInt(min, max + 1);
			})
			.register();
	}
}
