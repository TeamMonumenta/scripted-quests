package com.playmonumenta.scriptedquests.scriptedtimer;

import me.Novalescent.Constants;
import me.Novalescent.utils.Utils;
import me.Novalescent.utils.quadtree.reworked.QuadTreeValue;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import java.util.concurrent.TimeUnit;

public class TimerTreeNode extends QuadTreeValue {

	private String mMessage;
	private Timer mTimer;

	private ArmorStand mStand;
	public TimerTreeNode(Location loc, Timer timer, String message) {
		super(loc);
		mMessage = message;
		mTimer = timer;
	}

	public void spawn() {
		if (mStand != null) {
			mStand.remove();
			mStand = null;
		}

		mStand = getLocation().getWorld().spawn(getLocation(), ArmorStand.class, (ArmorStand stand) -> {
			stand.setVisible(false);
			stand.setMarker(true);
			stand.setInvulnerable(true);
			stand.setGravity(false);
			stand.addScoreboardTag(Constants.REMOVE_ONENABLE);

			long[] times = Utils.convertTicksIntoTime(mTimer.getTimeUntilReset() * 20);

			long days = times[3];
			long hours = times[2] + (days * 24);
			long minutes = times[1];
			long seconds = times[0];

			stand.setCustomName(ChatColor.translateAlternateColorCodes('&',
				mMessage.replaceAll("@T", ChatColor.of("#75c8ff") + "" + hours + ":" + minutes + ":" + seconds)));
			stand.setCustomNameVisible(true);
		});
	}

	public void despawn() {
		if (mStand != null) {
			mStand.remove();
			mStand = null;
		}
	}

	@Override
	public void destroy() {
		despawn();
	}
}
