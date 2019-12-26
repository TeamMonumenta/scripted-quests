package com.playmonumenta.scriptedquests.races;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.util.Vector;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.managers.RaceManager;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.RaceUtils;

/*
 * A Race is a currently active race a player is doing
 */
public class Race {
	private static final int NUM_RING_POINTS = 18;
	private static final List<Vector> RING_SHAPE = new ArrayList<Vector>(NUM_RING_POINTS);

	static {
		for (int angle = 0; angle < NUM_RING_POINTS; angle++) {
			RING_SHAPE.add(new Vector(Math.cos(Math.toRadians(angle * 360 / NUM_RING_POINTS)),
			Math.sin(Math.toRadians(angle * 360 / NUM_RING_POINTS)), 0));
		}
	}

	/* Arguments */
	private final Plugin mPlugin;
	private final RaceManager mManager;
	private final Player mPlayer;
	private final String mName;
	private final String mLabel;
	private final Objective mScoreboard;
	private final boolean mShowStats;
	private final Location mStart;
	private final QuestActions mStartActions;
	private final List<RaceWaypoint> mWaypoints;
	private final List<RaceTime> mTimes;
	private final QuestActions mLoseActions;

	/* Local constants */
	private final List<ArmorStand> mRingEntities = new ArrayList<ArmorStand>(NUM_RING_POINTS);
	private final World mWorld;
	private final Location mStopLoc; // Location where player should tp back to on lose

	/* Mutable variables for this race */
	private Deque<RaceWaypoint> mRemainingWaypoints;
	private RaceWaypoint mNextWaypoint;
	private long mStartTime;
	private long mMaxTime;
	private int mFrame = 0;
	private TimeBar mTimeBar = null;
	private boolean mCountdownActive = false;
	private int mWRTime;

	public Race(Plugin plugin, RaceManager manager, Player player, String name, String label,
	            Objective scoreboard, boolean showStats, Location start, QuestActions startActions,
	            List<RaceWaypoint> waypoints, List<RaceTime> times, QuestActions loseActions) {
		mPlugin = plugin;
		mManager = manager;
		mPlayer = player;
		mName = name;
		mLabel = label;
		mScoreboard = scoreboard;
		mShowStats = showStats;
		mStart = start;
		mStartActions = startActions;
		mWaypoints = waypoints;
		mTimes = times;
		mLoseActions = loseActions;

		mWorld = player.getWorld();
		mStopLoc = player.getLocation();
		mWRTime = getWRTime();

		// Create the ring entities in the right shape
		Location baseLoc = mWaypoints.get(0).getPosition().toLocation(mWorld);
		for (int angle = 0; angle < NUM_RING_POINTS; angle++) {
			ArmorStand out = (ArmorStand)mWorld.spawnEntity(baseLoc, EntityType.ARMOR_STAND);
			out.setHelmet(new ItemStack(Material.DIAMOND_BLOCK));
			out.setGlowing(true);
			out.setGravity(false);
			out.setInvulnerable(true);
			out.setVisible(false);
			out.setSmall(true);
			out.setMarker(true);
			mRingEntities.add(out);
		}

		restart();

		animation();
	}

	public void restart() {
		if (mCountdownActive) {
			// Refuse this restart request if still starting
			return;
		}

		if (mTimeBar != null) {
			mTimeBar.cancel();
		}

		if (mStartActions != null) {
			mStartActions.doActions(mPlugin, mPlayer, null, null);
		}

		mStartTime = System.currentTimeMillis();
		mMaxTime = mTimes.get(mTimes.size() - 1).getTime();

		// Copy the waypoints to something local to work with
		mRemainingWaypoints = new ArrayDeque<RaceWaypoint>(mWaypoints);
		mNextWaypoint = mRemainingWaypoints.removeFirst();

		// Create the timetracking bar
		mTimeBar = new TimeBar(mPlayer, mTimes);

		countdownStart();
	}

	public void countdownStart() {
		mCountdownActive = true;

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				// Teleport player if they get too far away from the start while counting down
				if (mTicks < 60 && mPlayer.getLocation().distance(mStart) > 0.8) {
					mPlayer.teleport(mStart);
				}

				if (mTicks == 0) {
					// TODO once restarting race works, used to be if !no_ui
					// mPlayer.sendMessage("" + ChatColor.BLUE + "Reminder:\nShift + Left-Click: Abandon\nShift + Right-Click: Retry");

					// 3
					mPlayer.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "3", "", 0, 20, 0);
					mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 0.890899f);
				} else if (mTicks == 20) {
					// 2
					mPlayer.sendTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "2", "", 0, 20, 0);
					mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 0.890899f);
				} else if (mTicks == 40) {
					// 1
					mPlayer.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "1", "", 0, 20, 0);
					mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 0.890899f);
				} else if (mTicks == 60) {
					// Go chime & title
					mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1,  1.781797f);
					mPlayer.sendTitle(ChatColor.WHITE + "" + ChatColor.BOLD + "GO", "", 0, 4, 0);

					// Reset race start time and create time bar
					mStartTime = System.currentTimeMillis();
					mCountdownActive = false;
				} else if (mTicks > 60 && mTicks < 120 && (mTicks % 6 == 0)) {
					// Go (white)
					mPlayer.sendTitle(ChatColor.WHITE + "" + ChatColor.BOLD + "GO", "", 0, 4, 0);
				} else if (mTicks > 60 && mTicks < 120 && ((mTicks + 3) % 6 == 0)) {
					// Go (aqua)
					mPlayer.sendTitle(ChatColor.AQUA + "" + ChatColor.BOLD + "GO", "", 0, 4, 0);
				} else if (mTicks == 120) {
					// Clear title
					mPlayer.sendTitle(" ", "", 0, 20, 0);
					this.cancel();
				}

				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	public void tick() {
		double distance = mPlayer.getEyeLocation().toVector().distance(mNextWaypoint.getPosition());

		// Time remaining display
		int timeElapsed = (int)(System.currentTimeMillis() - mStartTime);
		if (mCountdownActive) {
			timeElapsed = 0;
		}
		if (timeElapsed > mMaxTime) {
			mPlayer.sendMessage("" + ChatColor.RED + ChatColor.BOLD + "You ran out of time!");
			lose();
		}
		mTimeBar.update(timeElapsed);

		// Check if player went too far away
		if (distance > 100) {
			mPlayer.sendMessage("" + ChatColor.RED + ChatColor.BOLD + "You went too far away from the race path!");
			lose();
		} else if (distance < 4) {
			// TODO: Tell the player if they are going faster or slower than before
			//possibleRingTimes.add(timeElapsed);
			//if (has_ring_times) {
			//  int oldtime = ringTimes.get(actualRing);
			//  if (oldtime < timeElapsed) {
			//		MessagingUtils.sendActionBarMessage(mPlayer, net.md_5.bungee.api.ChatColor.RED, true, RaceUtils.msToTimeString(timeElapsed));
			//  } else {
			//		MessagingUtils.sendActionBarMessage(mPlayer, net.md_5.bungee.api.ChatColor.GREEN, true, RaceUtils.msToTimeString(timeElapsed));
			//  }
			//} else {

			// Run the actions for reaching this ring
			mNextWaypoint.doActions(mPlugin, mPlayer);

			MessagingUtils.sendActionBarMessage(mPlayer, net.md_5.bungee.api.ChatColor.BLUE, true, RaceUtils.msToTimeString(timeElapsed));
			mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1.5f);

			if (mRemainingWaypoints.size() == 0) {
				win(timeElapsed);
			} else {
				mNextWaypoint = mRemainingWaypoints.removeFirst();
			}
		}

		animation();
	}

	/*
	 * TODO (low priority)
	 * This creates a lot of new objects every tick - would be nice to optimize
	 */
	private void animation() {
		mFrame++;

		Location rloc = mNextWaypoint.getPosition().toLocation(mWorld);

		// Ring location faces player
		rloc.setDirection(mPlayer.getLocation().toVector().subtract(mNextWaypoint.getPosition()));
		int i = 0;
		for (Location loc : RaceUtils.transformPoints(rloc, RING_SHAPE, rloc.getYaw(), -rloc.getPitch(), 0d, 4 + 0.5 * (Math.cos(Math.toRadians(mFrame * 20))))) {
			loc.subtract(0, 0.5, 0);
			mRingEntities.get(i).teleport(loc);
			i++;
		}
	}

	/*
	 * This should only be called from win() and lose()
	 */
	private void end() {
		mManager.__removeRace(mPlayer);

		mTimeBar.cancel();
		for (Entity e : mRingEntities) {
			e.remove();
		}
	}

	public void lose() {
		if (mLoseActions != null) {
			mLoseActions.doActions(mPlugin, mPlayer, null, null);
		}
		mPlayer.teleport(mStopLoc);
		end();
	}

	/*
	 * Called by race manager when cancelling all races
	 */
	public void abort() {
		if (mLoseActions != null) {
			mLoseActions.doActions(mPlugin, mPlayer, null, null);
		}
		mPlayer.teleport(mStopLoc);
		mTimeBar.cancel();
		for (Entity e : mRingEntities) {
			e.remove();
		}
	}

	private void win(int endTime) {
		end();

		/* Set score on player */
		if (mScoreboard != null) {
			Score score = mScoreboard.getScore(mPlayer.getName());
			if (!score.isScoreSet() || score.getScore() == 0 || endTime < score.getScore()) {
				score.setScore(endTime);
				// handle new world record
				if (mWRTime > endTime) {
					String cmdStr = "broadcastcommand tellraw @a [\"\",{\"text\":\"" +
						mPlayer.getName() +
						"\",\"color\":\"blue\"},{\"text\":\" broke a new world record time for \",\"color\":\"dark_aqua\"},{\"text\":\"" +
						mName +
						"\",\"color\":\"blue\"},{\"text\":\"\\nNew time: \",\"color\":\"dark_aqua\"},{\"text\":\""+
						RaceUtils.msToTimeString(endTime) +
						"\",\"color\":\"blue\"}]";
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdStr);
				}
			}
		}

		//TODO: Ring times
		/*
		int pb = (has_ring_times ? ringTimes.get(ringTimes.size() - 1) : possibleRingTimes.get(possibleRingTimes.size() - 1));
		if (!has_ring_times || endTime < pb) { // if time beated
			pb = endTime;
			//rewrite recoded ringtimes
			Path path = Paths.get(plugin.getDataFolder().toString() +  "/speedruns" + File.separator + "playerdata/recorded_ring_times" + File.separator + baseFileName.toLowerCase() + File.separator + mPlayer.getName() + ".recorded");
			try {
				Files.deleteIfExists(path);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				List<String> newList = new ArrayList<String>(possibleRingTimes.size());
				for (Integer myInt : possibleRingTimes) {
					newList.add(String.valueOf(myInt));
				}
				Files.createDirectories(path.getParent());
				Files.createFile(path);
				Files.write(path, newList, Charset.forName("UTF-8"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			// update leaderboards
			String[] args = {"leaderboard", "add", baseFileName, Integer.toString(endTime)};
			new Leaderboard(plugin).leaderboard(mPlayer, args);
		}
		*/

		// display race end info
		//header
		if (mShowStats) {
			mPlayer.sendMessage("" + ChatColor.DARK_AQUA + ChatColor.BOLD +   "----====----       " + ChatColor.AQUA + ChatColor.BOLD + "Speedruns" + ChatColor.DARK_AQUA  + ChatColor.BOLD + "       ----====----\n");
			mPlayer.sendMessage(" " +  String.format("%s - %s", "" + ChatColor.AQUA + "Race Recap", " " + ChatColor.YELLOW + mName));
			mPlayer.sendMessage(" ");
		}

		//TODO: World record time first

		mPlayer.sendMessage(String.format("  %sWorld Record - %16s  | %s %s",
										  "" + ChatColor.AQUA + ChatColor.BOLD,
										  "" + RaceUtils.msToTimeString(mWRTime),
										  "" + ((endTime <= mWRTime) ? ("" + ChatColor.AQUA + ChatColor.BOLD + "\u272A") : ("" + ChatColor.GRAY + ChatColor.BOLD + "\u272A")),
										  "" + ((endTime <= mWRTime) ? ("" + ChatColor.BLUE + ChatColor.BOLD + "( -" + RaceUtils.msToTimeString(mWRTime - endTime) + ")") : ("" + ChatColor.RED + ChatColor.BOLD + "( +" + RaceUtils.msToTimeString(endTime - mWRTime) + ")"))));

		int bestMedalTime = Integer.MAX_VALUE;
		String bestMedalColor = null;
		for (RaceTime time : mTimes) {
			int medalTime = time.getTime();
			if (mShowStats) {
				mPlayer.sendMessage(String.format("  %s   %s      - %16s  | %s %s",
				                                  "" + time.getColor(),
				                                  "" + time.getLabel(),
				                                  "" + RaceUtils.msToTimeString(medalTime),
				                                  "" + ((endTime <= medalTime) ? ("" + time.getColor() + "\u272A") : ("" + ChatColor.GRAY + ChatColor.BOLD + "\u272A")),
				                                  "" + ((endTime <= medalTime) ? ("" + ChatColor.BLUE + ChatColor.BOLD + "( -" + RaceUtils.msToTimeString(medalTime - endTime) + ")") : ("" + ChatColor.RED + ChatColor.BOLD + "( +" + RaceUtils.msToTimeString(endTime - medalTime) + ")"))));
			}

			if (endTime <= medalTime) {
				if (medalTime < bestMedalTime) {
					bestMedalTime = medalTime;
					bestMedalColor = time.getColor();
				}
			}
		}

		if (mShowStats) {
			mPlayer.sendMessage(" ");
		}

		if (mScoreboard != null && mShowStats) {
			int personalBest = mScoreboard.getScore(mPlayer.getName()).getScore();
			mPlayer.sendMessage(String.format("  %s Personal Best - %16s  | %s",
											  "" + ChatColor.BLUE + ChatColor.BOLD,
											  "" + RaceUtils.msToTimeString(personalBest),
											  "" + ((endTime <= personalBest) ? ("" + ChatColor.BLUE + ChatColor.BOLD + "( -" + RaceUtils.msToTimeString(personalBest - endTime) + ")") : ("" + ChatColor.RED + ChatColor.BOLD + "( +" + RaceUtils.msToTimeString(endTime - personalBest) + ")"))));
		}

		if (mShowStats) {
			mPlayer.sendMessage(String.format("  %s  Your Time   - %16s",
			                                  "" + ChatColor.BLUE + ChatColor.BOLD,
			                                  "" + ChatColor.ITALIC + bestMedalColor + RaceUtils.msToTimeString(endTime)));
		}

		/* Last thing is to do any actions associated with the race */
		for (RaceTime time : mTimes) {
			if (endTime <= time.getTime()) {
				time.doActions(mPlugin, mPlayer);
			}
		}
	}

	private int getWRTime() {
		int top = Integer.MAX_VALUE;
		int score;
		for(String name : mScoreboard.getScoreboard().getEntries()) {
			score = mScoreboard.getScore(name).getScore();
			if(score < top && score > 0) {
				top = score;
			}
		}
		return top;
	}
}
