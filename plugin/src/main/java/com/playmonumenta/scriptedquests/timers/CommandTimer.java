package com.playmonumenta.scriptedquests.timers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.scriptedquests.Plugin;

public class CommandTimer implements Listener {
	private final Plugin mPlugin;
	private final Map<UUID, CommandTimerInstance> mTimers;
	private final BukkitRunnable mRunnable;
	private final int mPeriod;
	private final String mPeriodStr;

	public CommandTimer(Plugin plugin, int period) {
		mPlugin = plugin;
		mPeriod = period;
		mTimers = new LinkedHashMap<UUID, CommandTimerInstance>();

		if (mPeriod % 1200 == 0) {
			mPeriodStr = ChatColor.BLUE + Integer.toString(mPeriod / 1200) + "m";
		} else if (mPeriod % 20 == 0) {
			mPeriodStr = ChatColor.BLUE + Integer.toString(mPeriod / 20) + "s";
		} else {
			mPeriodStr = ChatColor.BLUE + Integer.toString(mPeriod) + "t";
		}

		mRunnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (mTimers.size() <= 0) {
					return;
				}

				for (CommandTimerInstance timer : mTimers.values()) {
					timer.tick(plugin);
				}
			}
		};

		mRunnable.runTaskTimer(plugin, 0, period);
	}

	/********************************************************************************
	 * Public Methods
	 *******************************************************************************/

	public void unload(Entity entity) {
		CommandTimerInstance timer = mTimers.get(entity.getUniqueId());
		if (timer != null) {
			timer.unload(mPlugin);
			mTimers.remove(entity.getUniqueId());
		}
	}

	public void unloadAndAbort() {
		for (CommandTimerInstance timer : mTimers.values()) {
			timer.unload(mPlugin);
		}
		mTimers.clear();
		mRunnable.cancel();
	}

	public void addEntity(final ArmorStand entity, final Set<String> tags) {
		/* This should never happen */
		if (mTimers.get(entity.getUniqueId()) != null) {
			mPlugin.getLogger().warning("processEntity: Attempted to add timer entity that was already tracked!");
			return;
		}

		int playerRange = -1;

		for (String tag : tags) {
			if (tag.startsWith("range=")) {
				playerRange = Integer.parseInt(tag.substring(6));
			}
		}

		Location loc = entity.getLocation().subtract(0, 1, 0);

		CommandTimerInstance timer;

		if (tags.contains("repeat")) {
			if (loc.getBlock().getType().equals(Material.REPEATING_COMMAND_BLOCK)) {
				timer = new CommandTimerInstance(loc, mPeriodStr, playerRange, true);
			} else {
				entity.setCustomNameVisible(true);
				entity.setCustomName(ChatColor.RED + "" + ChatColor.BOLD + "Timer: INVALID BLOCK");
				mPlugin.getLogger().warning("Timer is missing repeating command block at " + loc.toString());
				return;
			}
		} else {
			if (loc.getBlock().getType().equals(Material.COMMAND_BLOCK)) {
				timer = new CommandTimerInstance(loc, mPeriodStr, playerRange, false);
			} else {
				entity.setCustomNameVisible(true);
				entity.setCustomName(ChatColor.RED + "" + ChatColor.BOLD + "Timer: INVALID BLOCK");
				mPlugin.getLogger().warning("Timer is missing impulse command block at " + loc.toString());
				return;
			}
		}

		mTimers.put(entity.getUniqueId(), timer);

		entity.setInvulnerable(true);

		if (mPlugin.mShowTimerNames != null) {
			/* If configured to show or hide timer names, do so */
			entity.setCustomNameVisible(mPlugin.mShowTimerNames);
			if (mPlugin.mShowTimerNames) {
				/* If showing names, rename the armor stand to match what it actually does */
				timer.setName(entity);
			}
		}
	}

	public void tellTimers(CommandSender sender, boolean enabledOnly) {
		for (CommandTimerInstance timer : mTimers.values()) {
			if (!enabledOnly || timer.canRun()) {
				sender.sendMessage(timer.toString());
			}
		}
	}
}
