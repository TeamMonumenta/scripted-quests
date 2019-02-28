package com.playmonumenta.scriptedquests.timers;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.scriptedquests.Plugin;

public class CommandTimer implements Listener {
	private static class CommandTimerInstance {
		private final Location mLoc;
		private final int mPlayerRange;
		private final boolean mRepeat;

		protected CommandTimerInstance(Location loc, int playerRange, boolean repeat) {
			mLoc = loc;
			mPlayerRange = playerRange;
			mRepeat = repeat;
		}

		private static boolean isPlayerNearby(Location loc, double radius) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (loc.distance(player.getLocation()) <= radius && player.getGameMode() != GameMode.SPECTATOR) {
					return true;
				}
			}
			return false;
		}

		protected void tick(Plugin plugin) {
			if (mPlayerRange <= 0 || isPlayerNearby(mLoc, mPlayerRange)) {
				setAutoState(plugin, mLoc, true);
			} else if (mRepeat) {
				setAutoState(plugin, mLoc, false);
			}
		}

		private static java.lang.reflect.Method cachedHandleMethod = null;
		private static java.lang.reflect.Method cachedAutoMethod = null;
		private static void setAutoState(Plugin plugin, Location loc, boolean auto) {
			Block block = loc.getBlock();
			BlockState state = block.getState();
			if (state instanceof CommandBlock) {
				/*
				 * Use reflection to:
				 *   TileEntityCommand nmsTileCmd = cmd.getTileEntity();
				 *   nmsTileCmd.b(true)
				 */
				try {
					// Get command block's getTileEntity() via reflection
					// Cache reflection results for performance
					if (cachedHandleMethod == null) {
						cachedHandleMethod = state.getClass().getMethod("getTileEntity");
					}
					Object handle = cachedHandleMethod.invoke(state);

					if (cachedAutoMethod == null) {
						cachedAutoMethod = handle.getClass().getMethod("b", boolean.class);
					}
					cachedAutoMethod.invoke(handle, auto);
				} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
					plugin.getLogger().severe("Failed to set command block auto state at " + loc.toString());
					e.printStackTrace();
				}
			} else {
				plugin.getLogger().severe("Command block is missing for timer at " + loc.toString());
				return;
			}
		}
	}

	private final Plugin mPlugin;
	private final Map<UUID, CommandTimerInstance> mTimers;
	private final BukkitRunnable mRunnable;

	public CommandTimer(Plugin plugin, int period) {
		mPlugin = plugin;
		mTimers = new LinkedHashMap<UUID, CommandTimerInstance>();
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
		mTimers.remove(entity.getUniqueId());
	}

	public void unloadAndAbort() {
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
				timer = new CommandTimerInstance(loc, playerRange, true);
			} else {
				mPlugin.getLogger().warning("Timer is missing repeating command block at " + loc.toString());
				return;
			}
		} else {
			if (loc.getBlock().getType().equals(Material.COMMAND_BLOCK)) {
				timer = new CommandTimerInstance(loc, playerRange, false);
			} else {
				mPlugin.getLogger().warning("Timer is missing impulse command block at " + loc.toString());
				return;
			}
		}

		mTimers.put(entity.getUniqueId(), timer);
		if (mPlugin.mShowTimerNames != null) {
			entity.setCustomNameVisible(mPlugin.mShowTimerNames);
		}
	}
}
