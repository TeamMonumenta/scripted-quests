package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;

import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.GreedyStringArgument;
import io.github.jorelali.commandapi.api.arguments.IntegerArgument;
import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.bukkit.Bukkit;

public class DelayedCommand {
	private class DelayedCommandEntry {
		protected int ticksLeft;
		protected String command;

		protected DelayedCommandEntry(int ticksLeftIn, String commandIn) {
			ticksLeft = ticksLeftIn;
			command = commandIn;
		}

		protected void run() {
			mPlugin.getServer().dispatchCommand(mPlugin.getServer().getConsoleSender(), command);
		}
	}

	private final List<DelayedCommandEntry> mCommands = new ArrayList<DelayedCommandEntry>();
	private final Plugin mPlugin;
	private Integer mTaskId = null;
	private final Runnable mRunnable = new Runnable() {
		@Override
		public void run() {
			Iterator<DelayedCommandEntry> it = mCommands.iterator();
			while (it.hasNext())
			{
				DelayedCommandEntry entry = it.next();
				entry.ticksLeft--;

				if (entry.ticksLeft < 0) {
					entry.run();

					it.remove();
				}
			}

			if (mCommands.isEmpty()) {
				Bukkit.getScheduler().cancelTask(mTaskId);
				mTaskId = null;
			}
		}
	};

	@SuppressWarnings("unchecked")
	public DelayedCommand(Plugin plugin) {
		mPlugin = plugin;
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("ticks", new IntegerArgument(0));
		arguments.put("command", new GreedyStringArgument());

		CommandAPI.getInstance().register("delayedcommand",
		                                  new CommandPermission("scriptedquests.delayedcommand"),
		                                  arguments,
		                                  (sender, args) -> {
											  addDelayedCommand((Integer)args[0], (String)args[1]);
		                                  }
		);
	}

	private void addDelayedCommand(int ticks, String command) {
		mCommands.add(new DelayedCommandEntry(ticks, command));

		if (mTaskId == null) {
			mTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(mPlugin, mRunnable, 0, 1);
		}
	}

	/* Run all the remaining commands now, even though they are scheduled for later */
	public void cancel() {
		for (DelayedCommandEntry entry : mCommands) {
			entry.run();
		}
		mCommands.clear();

		if (mTaskId != null) {
			Bukkit.getScheduler().cancelTask(mTaskId);
		}
	}
}
