package com.playmonumenta.scriptedquests.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
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

	public static void register() {
		new CommandAPICommand("clock")
			.withPermission(CommandPermission.fromString("scriptedquests.clock"))
			.withArguments(new LocationArgument("location", LocationType.BLOCK_POSITION))
			.withArguments(new MultiLiteralArgument("always"))
			.withArguments(new IntegerArgument("period", 1))
			.executes((sender, args) -> {
				Location loc = (Location) args[0];
				loc.add(0.5, 0.0, 0.5);
				int period = (Integer) args[2];

				SummonTimerConsumer consumer = new SummonTimerConsumer(period);
				loc.getWorld().spawn(loc, ArmorStand.class, consumer);
			})
			.register();

		new CommandAPICommand("clock")
			.withPermission(CommandPermission.fromString("scriptedquests.clock"))
			.withArguments(new LocationArgument("location", LocationType.BLOCK_POSITION))
			.withArguments(new MultiLiteralArgument("always"))
			.withArguments(new IntegerArgument("period", 1))
			.withArguments(new MultiLiteralArgument("repeater"))
			.executes((sender, args) -> {
				Location loc = (Location) args[0];
				loc.add(0.5, 0.0, 0.5);
				int period = (Integer) args[2];

				SummonTimerConsumer consumer = new SummonTimerConsumer(period).setRepeater();
				loc.getWorld().spawn(loc, ArmorStand.class, consumer);
			})
			.register();

		new CommandAPICommand("clock")
			.withPermission(CommandPermission.fromString("scriptedquests.clock"))
			.withArguments(new LocationArgument("location", LocationType.BLOCK_POSITION))
			.withArguments(new MultiLiteralArgument("range"))
			.withArguments(new IntegerArgument("range", 1))
			.withArguments(new IntegerArgument("period", 1))
			.executes((sender, args) -> {
				Location loc = (Location) args[0];
				loc.add(0.5, 0.0, 0.5);
				int range = (Integer) args[2];
				int period = (Integer) args[3];

				SummonTimerConsumer consumer = new SummonTimerConsumer(period).setRange(range);
				loc.getWorld().spawn(loc, ArmorStand.class, consumer);
			})
			.register();

		new CommandAPICommand("clock")
			.withPermission(CommandPermission.fromString("scriptedquests.clock"))
			.withArguments(new LocationArgument("location", LocationType.BLOCK_POSITION))
			.withArguments(new MultiLiteralArgument("range"))
			.withArguments(new IntegerArgument("range", 1))
			.withArguments(new IntegerArgument("period", 1))
			.withArguments(new MultiLiteralArgument("repeater"))
			.executes((sender, args) -> {
				Location loc = (Location) args[0];
				loc.add(0.5, 0.0, 0.5);
				int range = (Integer) args[2];
				int period = (Integer) args[3];

				SummonTimerConsumer consumer = new SummonTimerConsumer(period).setRange(range).setRepeater();
				loc.getWorld().spawn(loc, ArmorStand.class, consumer);
			})
			.register();

		new CommandAPICommand("clock")
			.withPermission(CommandPermission.fromString("scriptedquests.clock"))
			.withArguments(new LocationArgument("location", LocationType.BLOCK_POSITION))
			.withArguments(new MultiLiteralArgument("player"))
			.withArguments(new IntegerArgument("period", 1))
			.executes((sender, args) -> {
				Location loc = (Location) args[0];
				loc.add(0.5, 0.0, 0.5);
				int period = (Integer) args[2];

				SummonTimerConsumer consumer = new SummonTimerConsumer(period).setPlayer();
				loc.getWorld().spawn(loc, ArmorStand.class, consumer);
			})
			.register();

		new CommandAPICommand("clock")
			.withPermission(CommandPermission.fromString("scriptedquests.clock"))
			.withArguments(new LocationArgument("location", LocationType.BLOCK_POSITION))
			.withArguments(new MultiLiteralArgument("player"))
			.withArguments(new IntegerArgument("period", 1))
			.withArguments(new MultiLiteralArgument("repeater"))
			.executes((sender, args) -> {
				Location loc = (Location) args[0];
				loc.add(0.5, 0.0, 0.5);
				int period = (Integer) args[2];

				SummonTimerConsumer consumer = new SummonTimerConsumer(period).setPlayer().setRepeater();
				loc.getWorld().spawn(loc, ArmorStand.class, consumer);
			})
			.register();
	}
}
