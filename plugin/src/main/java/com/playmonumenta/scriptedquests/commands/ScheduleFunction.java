package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.CommandArgument;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.FunctionArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class ScheduleFunction {
	private abstract static class DelayedAction implements Comparable<DelayedAction> {
		private int mTargetTick;

		@Override
		public int compareTo(DelayedAction other) {
			return mTargetTick - other.mTargetTick;
		}

		private DelayedAction(int ticksDelay) {
			mTargetTick = ticksDelay + Bukkit.getCurrentTick();
		}

		protected abstract void run();
	}

	private static class DelayedFunction extends DelayedAction {
		private final FunctionWrapper[] mFunction;

		private DelayedFunction(int ticksDelay, FunctionWrapper[] functionIn) {
			super(ticksDelay);
			mFunction = functionIn;
		}

		@Override
		protected void run() {
			for (FunctionWrapper func : mFunction) {
				func.run();
			}
		}

		@Override
		public String toString() {
			return "DelayedFunction{" +
				       "mFunction=" + Arrays.stream(mFunction).map(f -> f.getKey().toString()).collect(Collectors.joining(", ")) +
				       '}';
		}
	}

	private static class DelayedCommand extends DelayedAction {
		private final CommandSender mSender;
		private final String mCommand;

		private DelayedCommand(int ticksDelay, CommandSender sender, String command) {
			super(ticksDelay);
			mSender = sender;
			mCommand = command;
		}

		@Override
		protected void run() {
			CommandSender sender = mSender;
			if (sender instanceof ProxiedCommandSender) {
				sender = ((ProxiedCommandSender) sender).getCallee();
			}
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), sender instanceof Entity ? "execute as " + ((Entity) sender).getUniqueId() + " at @s run " + mCommand : mCommand);
		}

		@Override
		public String toString() {
			return "DelayedCommand{" +
				       "mSender=" + mSender +
				       ", mCommand='" + mCommand + '\'' +
				       '}';
		}
	}

	private final PriorityQueue<DelayedAction> mActions = new PriorityQueue<>();
	private final Plugin mPlugin;
	private @Nullable Integer mTaskId = null;
	private final Runnable mRunnable = new Runnable() {
		// Re-use the same temporary list each iteration
		private final List<DelayedAction> mActionsToRun = new ArrayList<>();

		@Override
		public void run() {
			Iterator<DelayedAction> it = mActions.iterator();
			while (it.hasNext()) {
				DelayedAction entry = it.next();
				int currentTick = Bukkit.getCurrentTick();

				if (entry.mTargetTick - currentTick <= 0) {
					mActionsToRun.add(entry);

					mPlugin.getLogger().fine("Preparing to run " + entry);
					it.remove();
				} else {
					break;
				}
			}

			for (DelayedAction entry : mActionsToRun) {
				entry.run();
			}
			mActionsToRun.clear();

			if (mActions.isEmpty()) {
				Bukkit.getScheduler().cancelTask(mTaskId);
				mTaskId = null;
			}
		}
	};

	public ScheduleFunction(Plugin plugin) {
		mPlugin = plugin;

		/* Unregister the default /schedule command */
		try {
			CommandAPI.unregister("schedule");
		} catch (Exception e) {
			// Nothing to do here - there is nothing to unregister in 1.13
			plugin.getLogger().info("Failed to unregister /schedule - this is only an error in 1.14+");
		}

		new CommandAPICommand("schedule")
			.withPermission(CommandPermission.fromString("scriptedquests.schedulefunction"))
			.withSubcommand(
				new CommandAPICommand("function")
					.withArguments(new FunctionArgument("function"),
					               new IntegerArgument("ticks", 0))
					.executes((sender, args) -> {
						addDelayedFunction((FunctionWrapper[]) args[0], (Integer) args[1]);
					}))
			.withSubcommand(
				new CommandAPICommand("command")
					.withArguments(new IntegerArgument("ticks", 0),
					               new CommandArgument("cmd"))
					.executes((sender, args) -> {
						addDelayedCommand(sender, (String) args[1], (Integer) args[0]);
					}))
			.register();

		/* TODO: Add the other /schedule variants (clear and replace/append variants) */
	}

	private void addDelayedAction(DelayedAction action) {
		mActions.add(action);

		if (mTaskId == null) {
			mTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(mPlugin, mRunnable, 0, 1);
		}
	}

	private void addDelayedFunction(FunctionWrapper[] function, int ticks) {
		addDelayedAction(new DelayedFunction(ticks, function));
	}

	private void addDelayedCommand(CommandSender sender, String command, int ticks) {
		addDelayedAction(new DelayedCommand(ticks, sender, command));
	}

	/* Run all the remaining commands now, even though they are scheduled for later */
	public void cancel() {
		for (DelayedAction entry : new PriorityQueue<>(mActions)) {
			// Note that these functions might add more functions... but there's nothing we can do about that
			entry.run();
		}
		mActions.clear();

		if (mTaskId != null) {
			Bukkit.getScheduler().cancelTask(mTaskId);
		}
	}
}
