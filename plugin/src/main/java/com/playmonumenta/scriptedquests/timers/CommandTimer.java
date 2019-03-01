package com.playmonumenta.scriptedquests.timers;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.scriptedquests.Plugin;

public class CommandTimer implements Listener {
	private static class CommandTimerInstance {
		private final Location mLoc;
		private final String mPeriodStr;
		private final int mPlayerRange;
		private final boolean mRepeat;

		/*
		 * The last state a repeater was set to - only used for repeaters
		 *
		 * Default to "on" so the first thing that happens after loading
		 * is to turn it off if no players in range
		 */
		private boolean mRepeaterEnabled = true;

		private CommandTimerInstance(Location loc, String periodStr, int playerRange, boolean repeat) {
			mLoc = loc;
			mPeriodStr = periodStr;
			mPlayerRange = playerRange;
			mRepeat = repeat;
		}

		private boolean canRun() {
			if (mPlayerRange <= 0) {
				return true;
			}

			for (Player player : Bukkit.getOnlinePlayers()) {
				if (mLoc.distance(player.getLocation()) <= mPlayerRange && player.getGameMode() != GameMode.SPECTATOR) {
					return true;
				}
			}
			return false;
		}

		private void tick(Plugin plugin) {
			if (canRun()) {
				setAutoState(plugin, mLoc, true);
				mRepeaterEnabled = true;
			} else if (mRepeat && mRepeaterEnabled) {
				/* Turn repeaters back off again when player is out of range */
				setAutoState(plugin, mLoc, false);
				mRepeaterEnabled = false;
			}
		}

		private void unload(Plugin plugin) {
			if (mRepeat && mRepeaterEnabled) {
				/* Turn repeaters back off when unloading timer */
				setAutoState(plugin, mLoc, false);
				mRepeaterEnabled = false;
			}
		}

		private void setName(ArmorStand entity) {
			entity.setCustomName(getName());
		}

		private String getName() {
			String name = "";
			if (mRepeat) {
				name += ChatColor.LIGHT_PURPLE + "Repeater ";
			} else {
				name += ChatColor.GOLD + "Timer ";
			}
			name += mPeriodStr + " ";
			if (mPlayerRange <= 0) {
				name += ChatColor.DARK_PURPLE + "always ";
			} else if (mPlayerRange > 1000) {
				name += ChatColor.YELLOW + "range=" + Integer.toString(mPlayerRange);
			} else {
				name += ChatColor.GREEN + "range=" + Integer.toString(mPlayerRange);
			}
			return name;
		}

		public String toString() {
			return Integer.toString(mLoc.getBlockX()) + " " +
			       Integer.toString(mLoc.getBlockY()) + " " +
			       Integer.toString(mLoc.getBlockZ()) + " " +
			       getName();
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
