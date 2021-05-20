package com.playmonumenta.scriptedquests.scriptedtimer;

import me.Novalescent.Constants;
import me.Novalescent.utils.Utils;
import me.Novalescent.utils.quadtree.reworked.QuadTreeValue;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TimerTreeNode extends QuadTreeValue {

	public final String mTimerTag;
	private String mMessage;
	public final Timer mTimer;

	private ArmorStand mStand;
	private BukkitRunnable mRunnable;
	public TimerTreeNode(String timerTag, Location loc, Timer timer, String message) {
		super(loc);
		mTimerTag = timerTag;
		mMessage = message;
		mTimer = timer;
	}

	public void spawn() {
		if (mStand != null) {
			mStand.remove();
			mStand = null;
			mRunnable.cancel();
			mRunnable = null;
		}

		mStand = getLocation().getWorld().spawn(getLocation(), ArmorStand.class, (ArmorStand stand) -> {
			stand.setVisible(false);
			stand.setMarker(true);
			stand.setInvulnerable(true);
			stand.setGravity(false);
			stand.addScoreboardTag(Constants.REMOVE_ONENABLE);

			Date date = new Date((long) (mTimer.getTimeUntilReset() * 1000));
			String formattedDate = new SimpleDateFormat("HH:mm:ss").format(date);

			stand.setCustomName(ChatColor.translateAlternateColorCodes('&',
				mMessage.replaceAll("@T", ChatColor.of("#75c8ff") + formattedDate)));
			stand.setCustomNameVisible(true);
		});

		mRunnable = new BukkitRunnable() {

			@Override
			public void run() {
				if (mStand == null || !mStand.isValid()) {
					 despawn();
					 return;
				}

				Date date = new Date((long) (mTimer.getTimeUntilReset() * 1000));
				String formattedDate = new SimpleDateFormat("HH:mm:ss").format(date);
				mStand.setCustomName(ChatColor.translateAlternateColorCodes('&',
					mMessage.replaceAll("@T", ChatColor.of("#75c8ff") + "" + formattedDate )));
			}

		};
		mRunnable.runTaskTimer(mTimer.mPlugin, 0, 20);
	}

	public void despawn() {
		if (mStand != null) {
			mStand.remove();
			mStand = null;
			mRunnable.cancel();
			mRunnable = null;
		}
	}

	public boolean isSpawned() {
		return mStand != null && mStand.isValid();
	}

	public ArmorStand getStand() {
		return mStand;
	}

	public String withLeading(long number) {
		String num = "" + number;
		while (num.length() <= 1) {
			num = "0" + num;
		}

		return num;
	}

	@Override
	public void destroy() {
		super.destroy();
		despawn();
	}
}
