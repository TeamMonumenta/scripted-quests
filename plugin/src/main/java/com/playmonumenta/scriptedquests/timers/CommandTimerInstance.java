package com.playmonumenta.scriptedquests.timers;

import java.lang.reflect.InvocationTargetException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Plugin;

public class CommandTimerInstance {
	private final Location mLoc;
	private final TimerCoords mCoords;
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

	public CommandTimerInstance(Location loc, TimerCoords coords, String periodStr, int playerRange, boolean repeat) {
		mLoc = loc;
		mCoords = coords;
		mPeriodStr = periodStr;
		mPlayerRange = playerRange;
		mRepeat = repeat;
	}

	public boolean canRun() {
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

	public void tick(Plugin plugin) {
		if (canRun()) {
			setAutoState(plugin, mLoc, true);
			mRepeaterEnabled = true;
		} else if (mRepeat && mRepeaterEnabled) {
			/* Turn repeaters back off again when player is out of range */
			setAutoState(plugin, mLoc, false);
			mRepeaterEnabled = false;
		}
	}

	public void unload(Plugin plugin) {
		if (mRepeat && mRepeaterEnabled) {
			/* Turn repeaters back off when unloading timer */
			setAutoState(plugin, mLoc, false);
			mRepeaterEnabled = false;
		}
	}

	public void setName(ArmorStand entity) {
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

	public TimerCoords getCoords() {
		return mCoords;
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

