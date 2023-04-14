package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.CommandArgument;
import com.playmonumenta.scriptedquests.utils.MMLog;
import com.playmonumenta.scriptedquests.utils.NmsUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.FunctionArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.TimeArgument;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class ScheduleFunction {
	private abstract static class DelayedAction implements Comparable<DelayedAction> {
		protected final int mTargetTick;
		private boolean mCancelled = false;

		@Override
		public int compareTo(DelayedAction other) {
			return Integer.compare(mTargetTick, other.mTargetTick);
		}

		private DelayedAction(int targetTick) {
			mTargetTick = targetTick;
		}

		protected boolean isCancelled() {
			return mCancelled;
		}

		protected void setCancelled() {
			mCancelled = true;
		}

		protected abstract void run();
	}

	private static class DelayedFunction extends DelayedAction {
		private final CommandSender mSender;
		private final FunctionWrapper mFunction;

		private DelayedFunction(int ticksDelay, CommandSender sender, FunctionWrapper functionIn) {
			super(ticksDelay);
			mSender = sender;
			mFunction = functionIn;
		}

		@Override
		protected void run() {
			if (isCancelled()) {
				return;
			}
			mFunction.run();
		}

		@Override
		public String toString() {
			return "DelayedFunction{" +
				       "mSender=" + mSender +
				       "mFunction=" + mFunction.getKey() +
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
			if (isCancelled()) {
				return;
			}
			CommandSender sender = mSender;
			if (sender instanceof ProxiedCommandSender) {
				sender = ((ProxiedCommandSender) sender).getCallee();
			}
			NmsUtils.getVersionAdapter().runConsoleCommandSilently(sender instanceof Entity ? "execute as " + ((Entity) sender).getUniqueId() + " at @s run " + mCommand : mCommand);
		}

		@Override
		public String toString() {
			return "DelayedCommand{" +
				       "mSender=" + mSender +
				       ", mCommand='" + mCommand + '\'' +
				       '}';
		}
	}

	private static class SenderDelayedTasks {
		private final Map<NamespacedKey, PriorityQueue<DelayedFunction>> mDelayedFunctions = new HashMap<>();

		// Monumenta add mode does not cancel any scheduled functions for this sender
		protected void addDelayedFunction(DelayedFunction delayedFunction) {
			NamespacedKey key = delayedFunction.mFunction.getKey();
			PriorityQueue<DelayedFunction> functionQueue = mDelayedFunctions.computeIfAbsent(key, k -> new PriorityQueue<>());
			functionQueue.add(delayedFunction);
		}

		// Vanilla append mode cancels functions scheduled on the same tick or earlier before scheduling
		protected void appendDelayedFunction(DelayedFunction delayedFunction) {
			NamespacedKey key = delayedFunction.mFunction.getKey();
			int targetTick = delayedFunction.mTargetTick;
			PriorityQueue<DelayedFunction> functions = mDelayedFunctions.get(key);
			if (functions != null) {
				Iterator<DelayedFunction> it = functions.iterator();
				while (it.hasNext()) {
					DelayedFunction testFunction = it.next();
					if (testFunction.mTargetTick > targetTick) {
						break;
					}
					testFunction.setCancelled();
					it.remove();
				}
			}
			addDelayedFunction(delayedFunction);
		}

		// Vanilla replace mode cancels all before scheduling
		protected void replaceDelayedFunction(DelayedFunction delayedFunction) {
			NamespacedKey key = delayedFunction.mFunction.getKey();
			clearDelayedFunction(key);
			addDelayedFunction(delayedFunction);
		}

		protected void removeDelayedFunction(DelayedFunction delayedFunction) {
			NamespacedKey key = delayedFunction.mFunction.getKey();

			PriorityQueue<DelayedFunction> functionQueue = mDelayedFunctions.get(key);
			if (functionQueue == null) {
				return;
			}

			functionQueue.remove(delayedFunction);
			if (functionQueue.isEmpty()) {
				mDelayedFunctions.remove(key);
			}
		}

		protected int clearDelayedFunction(NamespacedKey namespacedKey) {
			PriorityQueue<DelayedFunction> functionQueue = mDelayedFunctions.remove(namespacedKey);
			if (functionQueue == null) {
				return 0;
			}

			for (DelayedFunction function : functionQueue) {
				function.setCancelled();
			}
			return functionQueue.size();
		}

		protected boolean isEmpty() {
			return mDelayedFunctions.isEmpty();
		}
	}

	private final PriorityQueue<DelayedAction> mActions = new PriorityQueue<>();
	private final Map<CommandSender, SenderDelayedTasks> mSenderDelayedTasks = new HashMap<>();
	private final Plugin mPlugin;
	private @Nullable Integer mTaskId = null;
	private final Runnable mRunnable = new Runnable() {
		// Re-use the same temporary list each iteration
		private final List<DelayedAction> mActionsToRun = new ArrayList<>();

		@Override
		public void run() {
			int currentTick = Bukkit.getCurrentTick();
			Iterator<DelayedAction> it = mActions.iterator();
			while (it.hasNext()) {
				DelayedAction entry = it.next();

				if (entry.mTargetTick > currentTick) {
					break;
				}

				mActionsToRun.add(entry);
				if (entry instanceof DelayedFunction delayedFunction) {
					CommandSender sender = delayedFunction.mSender;
					SenderDelayedTasks senderDelayedTasks = mSenderDelayedTasks.get(sender);
					if (senderDelayedTasks == null) {
						MMLog.warning("[ScheduleFunction] Somehow running a delayed function for an unregistered sender");
					} else {
						senderDelayedTasks.removeDelayedFunction(delayedFunction);
						if (senderDelayedTasks.isEmpty()) {
							mSenderDelayedTasks.remove(sender);
						}
					}
				}
				mPlugin.getLogger().fine("Preparing to run " + entry);
				it.remove();
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
				new CommandAPICommand("command")
					.withArguments(new TimeArgument("time"),
						new CommandArgument("cmd"))
					.executes((sender, args) -> {
						return addDelayedCommand(sender, (String) args[1], (Integer) args[0]);
					}))
			.withSubcommand(
				new CommandAPICommand("function")
					.withArguments(new FunctionArgument("function"),
					               new TimeArgument("time"))
					.executes((sender, args) -> {
						return addDelayedFunction(sender, (FunctionWrapper[]) args[0], (Integer) args[1]);
					}))
			.withSubcommand(
				new CommandAPICommand("function")
					.withArguments(new FunctionArgument("function"),
						new TimeArgument("time"),
						new MultiLiteralArgument("add"))
					.executes((sender, args) -> {
						return addDelayedFunction(sender, (FunctionWrapper[]) args[0], (Integer) args[1]);
					}))
			.withSubcommand(
				new CommandAPICommand("function")
					.withArguments(new FunctionArgument("function"),
						new TimeArgument("time"),
						new MultiLiteralArgument("append"))
					.executes((sender, args) -> {
						return appendDelayedFunction(sender, (FunctionWrapper[]) args[0], (Integer) args[1]);
					}))
			.withSubcommand(
				new CommandAPICommand("function")
					.withArguments(new FunctionArgument("function"),
						new TimeArgument("time"),
						new MultiLiteralArgument("replace"))
					.executes((sender, args) -> {
						return replaceDelayedFunction(sender, (FunctionWrapper[]) args[0], (Integer) args[1]);
					}))
			.withSubcommand(
				new CommandAPICommand("clear")
					.withArguments(new FunctionArgument("function"))
					.executes((sender, args) -> {
						return clearDelayedFunction(sender, (FunctionWrapper[]) args[0]);
					}))
			.register();
	}

	private int getTargetTick(int ticksDelay) {
		return ticksDelay + Bukkit.getCurrentTick();
	}

	private void addDelayedAction(DelayedAction action) {
		mActions.add(action);

		if (mTaskId == null) {
			mTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(mPlugin, mRunnable, 0, 1);
		}
	}

	private int addDelayedFunction(CommandSender sender, FunctionWrapper[] functions, int ticks) {
		int targetTick = getTargetTick(ticks);
		SenderDelayedTasks senderDelayedTasks = mSenderDelayedTasks.computeIfAbsent(sender, k -> new SenderDelayedTasks());
		for (FunctionWrapper function : functions) {
			DelayedFunction delayedFunction = new DelayedFunction(targetTick, sender, function);
			senderDelayedTasks.addDelayedFunction(delayedFunction);
			addDelayedAction(delayedFunction);
		}
		return targetTick;
	}

	private int addDelayedCommand(CommandSender sender, String command, int ticks) {
		int targetTick = getTargetTick(ticks);
		addDelayedAction(new DelayedCommand(targetTick, sender, command));
		return targetTick;
	}

	private int appendDelayedFunction(CommandSender sender, FunctionWrapper[] functions, int ticks) {
		int targetTick = getTargetTick(ticks);
		SenderDelayedTasks senderDelayedTasks = mSenderDelayedTasks.computeIfAbsent(sender, k -> new SenderDelayedTasks());
		for (FunctionWrapper function : functions) {
			DelayedFunction delayedFunction = new DelayedFunction(targetTick, sender, function);
			senderDelayedTasks.appendDelayedFunction(delayedFunction);
			addDelayedAction(delayedFunction);
		}
		return targetTick;
	}

	private int replaceDelayedFunction(CommandSender sender, FunctionWrapper[] functions, int ticks) {
		int targetTick = getTargetTick(ticks);
		SenderDelayedTasks senderDelayedTasks = mSenderDelayedTasks.computeIfAbsent(sender, k -> new SenderDelayedTasks());
		for (FunctionWrapper function : functions) {
			DelayedFunction delayedFunction = new DelayedFunction(targetTick, sender, function);
			senderDelayedTasks.replaceDelayedFunction(delayedFunction);
			addDelayedAction(delayedFunction);
		}
		return targetTick;
	}

	private int clearDelayedFunction(CommandSender sender, FunctionWrapper[] functions) {
		int cleared = 0;
		SenderDelayedTasks senderDelayedTasks = mSenderDelayedTasks.get(sender);
		if (senderDelayedTasks != null) {
			for (FunctionWrapper function : functions) {
				cleared += senderDelayedTasks.clearDelayedFunction(function.getKey());
			}
			if (senderDelayedTasks.isEmpty()) {
				mSenderDelayedTasks.remove(sender);
			}
		}
		return cleared;
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
