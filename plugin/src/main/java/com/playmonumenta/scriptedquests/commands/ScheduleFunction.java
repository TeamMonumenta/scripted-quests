package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.senderid.SenderId;
import com.playmonumenta.scriptedquests.utils.CommandArgument;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.FunctionArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public class ScheduleFunction implements EventListener {
	private abstract static class DelayedAction implements Comparable<DelayedAction> {
		private final SenderId mSenderId;
		private final int mTargetTick;
		private boolean mCancelled = false;

		@Override
		public int compareTo(DelayedAction other) {
			return mTargetTick - other.mTargetTick;
		}

		private DelayedAction(CommandSender sender, int ticksDelay) {
			mSenderId = SenderId.of(sender);
			mTargetTick = ticksDelay + Bukkit.getCurrentTick();
		}

		public SenderId senderId() {
			return mSenderId;
		}

		public void setCancelled() {
			mCancelled = true;
		}

		public boolean isCancelled() {
			return mCancelled;
		}

		protected abstract void run();

		@Override
		public abstract String toString();
	}

	private static class DelayedFunction extends DelayedAction {
		private final FunctionWrapper[] mFunction;

		private DelayedFunction(int ticksDelay, CommandSender sender, FunctionWrapper[] functionIn) {
			super(sender, ticksDelay);
			mFunction = functionIn;
		}

		@Override
		protected void run() {
			if (isCancelled()) {
				return;
			}
			if (!senderId().isLoaded()) {
				return;
			}

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
		private final String mCommand;

		private DelayedCommand(int ticksDelay, CommandSender sender, String command) {
			super(sender, ticksDelay);
			mCommand = command;
		}

		@Override
		protected void run() {
			if (isCancelled()) {
				return;
			}
			if (!senderId().isLoaded()) {
				return;
			}

			CommandSender sender = senderId().callee();
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), sender instanceof Entity ? "execute as " + ((Entity) sender).getUniqueId() + " at @s run " + mCommand : mCommand);
		}

		@Override
		public String toString() {
			return "DelayedCommand{" +
				       "mSenderId=" + senderId() +
				       ", mCommand='" + mCommand + '\'' +
				       '}';
		}
	}

	private final static PriorityQueue<DelayedAction> mActions = new PriorityQueue<>();
	private final static Map<SenderId, PriorityQueue<DelayedAction>> mSenderActions = new HashMap<>();
	private static Plugin mPlugin;
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
				SenderId senderId = entry.senderId();
				@Nullable PriorityQueue<DelayedAction> senderActions = mSenderActions.get(senderId);
				if (senderActions != null) {
					senderActions.remove(entry);
					if (senderActions.isEmpty()) {
						mSenderActions.remove(senderId);
					}
				}
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
						addDelayedFunction(sender, (FunctionWrapper[]) args[0], (Integer) args[1]);
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
		SenderId senderId = action.senderId();
		PriorityQueue<DelayedAction> senderActions;
		senderActions = mSenderActions.computeIfAbsent(senderId, k -> new PriorityQueue<>());
		senderActions.add(action);

		if (mTaskId == null) {
			mTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(mPlugin, mRunnable, 0, 1);
		}
	}

	public static void cancelSenderActions(CommandSender sender) {
		SenderId senderId = SenderId.of(sender);
		@Nullable PriorityQueue<DelayedAction> senderActions;
		senderActions = mSenderActions.remove(senderId);
		if (senderActions != null) {
			mPlugin.getLogger().fine("Cancelling scheduled actions for " + senderId);
			for (DelayedAction action : senderActions) {
				mPlugin.getLogger().fine("- Cancelling action: " + action);
				action.setCancelled();
			}
		}
	}

	private void addDelayedFunction(CommandSender sender, FunctionWrapper[] function, int ticks) {
		addDelayedAction(new DelayedFunction(ticks, sender, function));
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
		mSenderActions.clear();

		if (mTaskId != null) {
			Bukkit.getScheduler().cancelTask(mTaskId);
		}
	}
}
