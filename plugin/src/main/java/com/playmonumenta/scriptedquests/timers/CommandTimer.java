package com.playmonumenta.scriptedquests.timers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
	private final TreeMap<TimerCoords, ArmorStand> mCoords = new TreeMap<>();

	private final Plugin mPlugin;
	private final ArrayList<LinkedHashMap<UUID, CommandTimerInstance>> mTickTimers;
	private final Map<UUID, CommandTimerInstance> mTimers;
	private final int mPeriod;
	private final String mPeriodStr;
	private int mCounter = 0;


	public CommandTimer(Plugin plugin, int period) {
		mPlugin = plugin;
		mPeriod = period;
		//List of timers sorted in their scheduled tick
		mTickTimers = new ArrayList<LinkedHashMap<UUID, CommandTimerInstance>>(mPeriod);
		//List of all timers with this period
		mTimers = new LinkedHashMap<UUID, CommandTimerInstance>();

		//Generate the timer buckets
		for (int i = 0; i < mPeriod; i++) {
			mTickTimers.add(new LinkedHashMap<UUID, CommandTimerInstance>());
		}

		if (mPeriod % 1200 == 0) {
			mPeriodStr = ChatColor.BLUE + Integer.toString(mPeriod / 1200) + "m";
		} else if (mPeriod % 20 == 0) {
			mPeriodStr = ChatColor.BLUE + Integer.toString(mPeriod / 20) + "s";
		} else {
			mPeriodStr = ChatColor.BLUE + Integer.toString(mPeriod) + "t";
		}


	}

	/********************************************************************************
	 * Public Methods
	 *******************************************************************************/

	public void unload(Entity entity) {
		CommandTimerInstance timer = mTimers.get(entity.getUniqueId());
		if (timer != null) {
			timer.unload(mPlugin);
			mCoords.remove(timer.getCoords());
			mTimers.remove(entity.getUniqueId());

			for (LinkedHashMap<UUID, CommandTimerInstance> map: mTickTimers) {
				map.remove(entity.getUniqueId());
			}
		}
	}

	public void unloadAndAbort() {
		for (CommandTimerInstance timer : mTimers.values()) {
			timer.unload(mPlugin);
		}
		mTimers.clear();
		for (LinkedHashMap<UUID, CommandTimerInstance> map: mTickTimers) {
			map.clear();
		}
		mCoords.clear();
	}

	public void addEntity(final ArmorStand entity, final Set<String> tags) {
		/* This should never happen */
		if (mTimers.get(entity.getUniqueId()) != null) {
			mPlugin.getLogger().warning("processEntity: Attempted to add timer entity that was already tracked!");
			return;
		}

		int playerRangeTemp = -1;

		for (String tag : tags) {
			if (tag.startsWith("range=")) {
				playerRangeTemp = Integer.parseInt(tag.substring(6));
				break;
			}
		}

		final int playerRange = playerRangeTemp;
		final Location loc = entity.getLocation().subtract(0, 1, 0);
		final TimerCoords coords = new TimerCoords(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

		if (!tryAddTimer(entity, tags, playerRange, loc, coords)) {
			// Adding initially failed, try exactly one more time in 10 ticks
			new BukkitRunnable() {
				@Override
				public void run() {
					tryAddTimer(entity, tags, playerRange, loc, coords);
				}
			}.runTaskLater(mPlugin, 10);
		}
	}

	/* Tries to add the timer. Returns true if successful, false if not */
	private boolean tryAddTimer(final ArmorStand entity, final Set<String> tags, final int playerRange, final Location loc, final TimerCoords coords) {
		CommandTimerInstance timer;

		if (tags.contains("repeat")) {
			if (loc.getBlock().getType().equals(Material.REPEATING_COMMAND_BLOCK)) {
				timer = new CommandTimerInstance(loc, coords, mPeriodStr, playerRange, true);
			} else {
				entity.setCustomNameVisible(true);
				entity.setCustomName(ChatColor.RED + "" + ChatColor.BOLD + "Timer: INVALID BLOCK");
				mPlugin.getLogger().warning("Timer is missing repeating command block at " + loc.toString());
				return false;
			}
		} else {
			if (loc.getBlock().getType().equals(Material.COMMAND_BLOCK)) {
				timer = new CommandTimerInstance(loc, coords, mPeriodStr, playerRange, false);
			} else {
				entity.setCustomNameVisible(true);
				entity.setCustomName(ChatColor.RED + "" + ChatColor.BOLD + "Timer: INVALID BLOCK");
				mPlugin.getLogger().warning("Timer is missing impulse command block at " + loc.toString());
				return false;
			}
		}

		/* Store a reference to the location where this timer sits */
		ArmorStand existing = mCoords.get(coords);
		if (existing != null) {
			/* There is already a timer here! Unload and kill it */
			unload(existing);
			existing.remove();
		}
		mCoords.put(coords, entity);

		mTimers.put(entity.getUniqueId(), timer);
		scheduleTimer(entity.getUniqueId(), timer);

		entity.setInvulnerable(true);

		if (mPlugin.mShowTimerNames != null) {
			/* If configured to show or hide timer names, do so */
			entity.setCustomNameVisible(mPlugin.mShowTimerNames);
			if (mPlugin.mShowTimerNames) {
				/* If showing names, rename the armor stand to match what it actually does */
				timer.setName(entity);
			}
		}

		return true;
	}

	//Put the new timer in a bucket
	//Find smallest bucket and place new timer in it
	private void scheduleTimer(UUID uniqueId, CommandTimerInstance timer) {

		LinkedHashMap<UUID, CommandTimerInstance> smallest = mTickTimers.get(0);

		for (LinkedHashMap<UUID, CommandTimerInstance> map: mTickTimers) {
			if (map.size() == 0) {
				map.put(uniqueId, timer);
				return;
			}
			if (map.size() < smallest.size()) {
				smallest = map;
			}
		}

		smallest.put(uniqueId, timer);
	}

	public void tellTimers(CommandSender sender, boolean enabledOnly) {
		for (CommandTimerInstance timer : mTimers.values()) {
			if (!enabledOnly || timer.canRun()) {
				sender.sendMessage(timer.toString());
			}
		}
	}

	public void runTimers() {
		if (mTimers.size() <= 0) {
			return;
		}

		for (CommandTimerInstance timer : mTickTimers.get(mCounter).values()) {
			timer.tick(mPlugin);
		}
		mCounter++;
		if (mCounter >= mPeriod) {
			mCounter = 0;
		}
	}
}
