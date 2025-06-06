package com.playmonumenta.scriptedquests.timers;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.MMLog;
import com.playmonumenta.scriptedquests.utils.NmsUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

public class CommandTimerInstance {
	private final Location mLoc;
	private final TimerCoords mCoords;
	private final String mPeriodStr;
	private final int mPlayerRange;
	private final boolean mPlayerOnline;
	private final boolean mRepeat;

	/*
	 * The last state a repeater was set to - only used for repeaters
	 *
	 * Default to "on" so the first thing that happens after loading
	 * is to turn it off if no players in range
	 */
	private boolean mRepeaterEnabled = true;

	public CommandTimerInstance(Location loc, TimerCoords coords, String periodStr, int playerRange, boolean playerOnline, boolean repeat) {
		MMLog.finer("Adding new timer with coords=" + coords + " period=" + periodStr + " range=" + playerRange + " playerOnline=" + playerOnline + " repeat=" + repeat);
		mLoc = loc;
		mCoords = coords;
		mPeriodStr = periodStr;
		mPlayerRange = playerRange;
		mPlayerOnline = playerOnline;
		mRepeat = repeat;
	}

	public boolean canRun() {
		if (mPlayerOnline) {
			return !Bukkit.getOnlinePlayers().isEmpty();
		}

		if (mPlayerRange <= 0) {
			return true;
		}

		for (Player player : mLoc.getWorld().getPlayers()) {
			if (mLoc.distance(player.getLocation()) <= mPlayerRange && player.getGameMode() != GameMode.SPECTATOR) {
				return true;
			}
		}
		return false;
	}

	public void tick(Plugin plugin) {
		if (canRun()) {
			setAutoState(plugin, mLoc, false);
			setAutoState(plugin, mLoc, true);
			mRepeaterEnabled = true;
		} else if (mRepeat && mRepeaterEnabled) {
			/* Turn repeaters back off again when player is out of range */
			setAutoState(plugin, mLoc, false);
			mRepeaterEnabled = false;
		}
	}

	public void unload(Plugin plugin) {
		MMLog.finer("Unloading timer at " + mLoc);
		if (mRepeat && mRepeaterEnabled) {
			/* Turn repeaters back off when unloading timer */
			setAutoState(plugin, mLoc, false);
			mRepeaterEnabled = false;
		}
	}

	public void setName(ArmorStand entity) {
		entity.customName(getName());
	}

	private Component getName() {
		Component name = Component.empty();

		if (mRepeat) {
			name = name.append(Component.text("Repeater " + mPeriodStr + " ", NamedTextColor.LIGHT_PURPLE));
		} else {
			name = name.append(Component.text("Timer " + mPeriodStr + " ", NamedTextColor.GOLD));
		}
		if (mPlayerOnline) {
			name = name.append(Component.text("player online", NamedTextColor.DARK_GREEN));
		} else if (mPlayerRange <= 0) {
			name = name.append(Component.text("always", NamedTextColor.DARK_PURPLE));
		} else if (mPlayerRange > 1000) {
			name = name.append(Component.text("range=" + Integer.toString(mPlayerRange), NamedTextColor.YELLOW));
		} else {
			name = name.append(Component.text("range=" + Integer.toString(mPlayerRange), NamedTextColor.GREEN));
		}
		return name;
	}

	public World getWorld() {
		return mLoc.getWorld();
	}

	@Override
	public String toString() {
		return Integer.toString(mLoc.getBlockX()) + " " +
			   Integer.toString(mLoc.getBlockY()) + " " +
			   Integer.toString(mLoc.getBlockZ()) + " " +
			   getName();
	}

	public TimerCoords getCoords() {
		return mCoords;
	}

	private static void setAutoState(Plugin plugin, Location loc, boolean auto) {
		Block block = loc.getBlock();
		BlockState state = block.getState();
		if (state instanceof CommandBlock commandBlock) {
			NmsUtils.getVersionAdapter().setAutoState(commandBlock, auto);
		} else {
			plugin.getLogger().severe("Command block is missing for timer at " + loc.toString());
		}
	}

}
