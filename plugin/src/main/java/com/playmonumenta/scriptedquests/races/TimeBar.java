package com.playmonumenta.scriptedquests.races;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public class TimeBar {
	private static final List<BarColor> BAR_COLORS = Arrays.asList(BarColor.BLUE, BarColor.GREEN, BarColor.YELLOW, BarColor.WHITE, BarColor.RED);

	private final List<RaceTime> mTimes;

	public BossBar mBar;

	public TimeBar(Player runner, List<RaceTime> times) {
		mTimes = times;

		mBar = Bukkit.getServer().createBossBar("", BarColor.WHITE, BarStyle.SOLID);
		mBar.addPlayer(runner);
		mBar.setVisible(true);
		update(0);
	}

	public void update(int curTime) {
		int prevMedalTime = 0;
		int barColorIndex = 0;
		for (RaceTime time : mTimes) {
			int medalTime = time.getTime();
			if (curTime > medalTime) {
				// Exceeded this time - remember it and go to next
				prevMedalTime = medalTime;
				if (barColorIndex < BAR_COLORS.size()) {
					barColorIndex++;
				}
				continue;
			}

			// Still something left we haven't exceeded - that is the current goal time
			double percent = 1 - ((double)(curTime - prevMedalTime) / (double)(medalTime - prevMedalTime));
			if (percent > 0) {
				mBar.setProgress(percent);
			}

			mBar.setTitle("Time Left: " + time.getColor() + time.getLabel());
			mBar.setColor(BAR_COLORS.get(barColorIndex));
			break;
		}
	}

	public void cancel() {
		mBar.removeAll();
		mBar.setVisible(false);
	}
}
