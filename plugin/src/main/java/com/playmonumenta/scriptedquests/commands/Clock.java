package com.playmonumenta.scriptedquests.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.executors.CommandArguments;
import java.util.function.Function;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.util.Consumer;

public class Clock {
	static class SummonTimerConsumer implements Consumer<ArmorStand> {
		private final int mPeriod;
		private int mRange = 0;
		private boolean mPlayer = false;
		private boolean mRepeater = false;

		public SummonTimerConsumer(int period) {
			mPeriod = period;
		}

		public SummonTimerConsumer setRange(int range) {
			if (range > 0) {
				mRange = range;
			} else {
				mRange = 0;
			}
			return this;
		}

		public SummonTimerConsumer setPlayer() {
			mPlayer = true;
			return this;
		}

		public SummonTimerConsumer setRepeater() {
			mRepeater = true;
			return this;
		}

		@Override
		public void accept(ArmorStand armorStand) {
			armorStand.setSmall(true);
			armorStand.addScoreboardTag("timer");
			armorStand.addScoreboardTag("period=" + mPeriod);
			if (mRange > 0) {
				armorStand.addScoreboardTag("range=" + mRange);
			} else if (mPlayer) {
				armorStand.addScoreboardTag("range=player");
			}
			if (mRepeater) {
				armorStand.addScoreboardTag("repeat");
			}
		}
	}

	private static final Argument<Location> locationArg = new LocationArgument("location", LocationType.BLOCK_POSITION);
	private static final Argument<Integer> periodArg = new IntegerArgument("period", 1);
	private static final Argument<Integer> rangeArg = new IntegerArgument("range", 1);

	public static void register() {
		new CommandAPICommand("clock")
			.withPermission(CommandPermission.fromString("scriptedquests.clock"))
			.withArguments(locationArg)
			.withArguments(new LiteralArgument("always"))
			.withArguments(periodArg)
			.executes((sender, args) -> {
				summonTimer(args, t -> t);
			})
			.register();

		new CommandAPICommand("clock")
			.withPermission(CommandPermission.fromString("scriptedquests.clock"))
			.withArguments(locationArg)
			.withArguments(new LiteralArgument("always"))
			.withArguments(periodArg)
			.withArguments(new LiteralArgument("repeater"))
			.executes((sender, args) -> {
				summonTimer(args, SummonTimerConsumer::setRepeater);
			})
			.register();

		new CommandAPICommand("clock")
			.withPermission(CommandPermission.fromString("scriptedquests.clock"))
			.withArguments(locationArg)
			.withArguments(new LiteralArgument("range"))
			.withArguments(rangeArg)
			.withArguments(periodArg)
			.executes((sender, args) -> {
				int range = args.getByArgument(rangeArg);
				summonTimer(args, t -> t.setRange(range));
			})
			.register();

		new CommandAPICommand("clock")
			.withPermission(CommandPermission.fromString("scriptedquests.clock"))
			.withArguments(locationArg)
			.withArguments(new LiteralArgument("range"))
			.withArguments(rangeArg)
			.withArguments(periodArg)
			.withArguments(new LiteralArgument("repeater"))
			.executes((sender, args) -> {
				int range = args.getByArgument(rangeArg);
				summonTimer(args, t -> t.setRange(range).setRepeater());
			})
			.register();

		new CommandAPICommand("clock")
			.withPermission(CommandPermission.fromString("scriptedquests.clock"))
			.withArguments(locationArg)
			.withArguments(new LiteralArgument("player"))
			.withArguments(periodArg)
			.executes((sender, args) -> {
				summonTimer(args, SummonTimerConsumer::setPlayer);
			})
			.register();

		new CommandAPICommand("clock")
			.withPermission(CommandPermission.fromString("scriptedquests.clock"))
			.withArguments(locationArg)
			.withArguments(new LiteralArgument("player"))
			.withArguments(periodArg)
			.withArguments(new LiteralArgument("repeater"))
			.executes((sender, args) -> {
				summonTimer(args, t -> t.setPlayer().setRepeater());
			})
			.register();
	}

	private static void summonTimer(CommandArguments args, Function<SummonTimerConsumer, SummonTimerConsumer> func) {
		Location loc = args.getByArgument(locationArg);
		loc.add(0.5, 0.0, 0.5);
		int period = args.getByArgument(periodArg);

		SummonTimerConsumer consumer = func.apply(new SummonTimerConsumer(period));
		loc.getWorld().spawn(loc, ArmorStand.class, consumer);
	}
}
