package com.playmonumenta.scriptedquests.races;

import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.managers.RaceManager;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.RaceUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.jetbrains.annotations.Nullable;

/*
 * A Race is a currently active race a player is doing
 */
public class Race {
	private static final int NUM_RING_POINTS = 18;
	private static final List<Vector> RING_SHAPE = new ArrayList<>(NUM_RING_POINTS);

	static {
		for (int angle = 0; angle < NUM_RING_POINTS; angle++) {
			RING_SHAPE.add(new Vector(Math.cos(Math.toRadians(angle * 360.0 / NUM_RING_POINTS)),
			Math.sin(Math.toRadians(angle * 360.0 / NUM_RING_POINTS)), 0));
		}
	}

	/* Arguments */
	private final Plugin mPlugin;
	private final RaceManager mManager;
	private final Player mPlayer;
	private final String mName;
	private final @Nullable Objective mScoreboard;
	private final boolean mShowStats;
	private final boolean mAllowDialogClick;
	private final boolean mAllowCode;
	private final boolean mAllowClickables;
	private final boolean mAllowNpcInteraction;
	private final boolean mRingless;
	private final double mMaxDistance;
	private final Location mStart;
	private final @Nullable QuestActions mStartActions;
	private final List<RaceWaypoint> mWaypoints;
	private final List<RaceTime> mTimes;
	private final @Nullable QuestActions mLoseActions;

	/* Local constants */
	private final List<ArmorStand> mRingEntities = new ArrayList<>(NUM_RING_POINTS);
	private final World mWorld;
	private final Location mStopLoc; // Location where player should tp back to on lose

	/* Mutable variables for this race */
	private Deque<RaceWaypoint> mRemainingWaypoints;
	private RaceWaypoint mNextWaypoint;
	private long mStartTime;
	private long mMaxTime;
	private int mFrame = 0;
	private @Nullable TimeBar mTimeBar = null;
	private boolean mCountdownActive = false;
	private int mWRTime = Integer.MAX_VALUE;

	public Race(Plugin plugin, RaceManager manager, Player player, String name,
	            @Nullable Objective scoreboard, boolean showStats, boolean ringless, Location start, @Nullable QuestActions startActions,
	            List<RaceWaypoint> waypoints, List<RaceTime> times, @Nullable QuestActions loseActions, boolean allowDialogClick,
				boolean allowCode, boolean allowClickables, boolean allowNpcInteraction, double maxDistance) {
		mPlugin = plugin;
		mManager = manager;
		mPlayer = player;
		mName = name;
		mScoreboard = scoreboard;
		mShowStats = showStats;
		mAllowDialogClick = allowDialogClick;
		mAllowCode = allowCode;
		mAllowClickables = allowClickables;
		mAllowNpcInteraction = allowNpcInteraction;
		mRingless = ringless;
		mMaxDistance = maxDistance;
		mStart = start;
		mStartActions = startActions;
		mWaypoints = waypoints;
		mTimes = times;
		mLoseActions = loseActions;

		mWorld = player.getWorld();
		mStopLoc = player.getLocation();

		updateWRTime();

		// Create the ring entities in the right shape
		Location baseLoc = mWaypoints.get(0).getPosition().toLocation(mWorld);
		for (int angle = 0; angle < NUM_RING_POINTS; angle++) {
			ArmorStand out = (ArmorStand)mWorld.spawnEntity(baseLoc, EntityType.ARMOR_STAND);
			out.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_BLOCK));
			out.setGlowing(true);
			out.setGravity(false);
			out.setInvulnerable(true);
			out.setVisible(false);
			out.setSmall(true);
			out.setMarker(true);
			out.addScoreboardTag(RaceManager.ARMOR_STAND_RACE_TAG);
			out.addScoreboardTag(RaceManager.ARMOR_STAND_ID_PREFIX_TAG + mPlayer.getUniqueId());
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
			mStartActions.doActions(new QuestContext(mPlugin, mPlayer, null));
		}

		mPlayer.addScoreboardTag(RaceManager.PLAYER_RACE_TAG);

		mStartTime = System.currentTimeMillis();
		mMaxTime = mTimes.get(mTimes.size() - 1).getTime();

		// Copy the waypoints to something local to work with
		mRemainingWaypoints = new ArrayDeque<>(mWaypoints);
		mNextWaypoint = mRemainingWaypoints.removeFirst();

		// Create the time-tracking bar
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
				Location pLoc = mPlayer.getLocation();

				// Compute the distance manually, with y distance counting only 1/3 as much as X and Z
				double dist = Math.sqrt(Math.pow(pLoc.getX() - mStart.getX(), 2) + Math.pow((pLoc.getY() - mStart.getY()) / 3, 2) + Math.pow(pLoc.getZ() - mStart.getZ(), 2));

				if (mTicks < 60 && dist > 0.8 && !mRingless && mCountdownActive) {
					mPlayer.teleport(mStart);
				}

				if (mTicks == 0) {
					if (!mShowStats || !mRingless) {
						mPlayer.sendMessage("" + ChatColor.BLUE + "Reminder:\nShift + Left-Click: Abandon\nShift + Right-Click: Retry");
					}
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
					mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1.781797f);
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

				// Cancel the task if the countdown is deactivated, to prevent
				// the titles to keep appearing even if a player left the race.
				if (mCountdownActive) {
					mTicks++;
				} else {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	public int getTimeElapsed() {
		return (int)(System.currentTimeMillis() - mStartTime);
	}

	public void tick() {
		double distance = mPlayer.getEyeLocation().toVector().distance(mNextWaypoint.getPosition());

		// Time remaining display
		int timeElapsed = this.getTimeElapsed();
		if (mCountdownActive) {
			timeElapsed = 0;
		}
		if (timeElapsed > mMaxTime) {
			mPlayer.sendMessage("" + ChatColor.RED + ChatColor.BOLD + "You ran out of time!");
			lose();
			return;
		}
		if (mTimeBar != null) {
			mTimeBar.update(timeElapsed);
		}

		if (!mRingless) {

			// Check if player went too far away
			if (distance > mMaxDistance) {
				mPlayer.sendMessage("" + ChatColor.RED + ChatColor.BOLD + "You went too far away from the race path!");
				lose();
				return;
			} else if (distance < mNextWaypoint.getRadius()) {
				// TODO: Tell the player if they are going faster or slower than before
				/*
				possibleRingTimes.add(timeElapsed);
				if (has_ring_times) {
					int oldtime = ringTimes.get(actualRing);
					if (oldtime < timeElapsed) {
						MessagingUtils.sendActionBarMessage(mPlayer, net.md_5.bungee.api.ChatColor.RED, true, RaceUtils.msToTimeString(timeElapsed));
					} else {
						MessagingUtils.sendActionBarMessage(mPlayer, net.md_5.bungee.api.ChatColor.GREEN, true, RaceUtils.msToTimeString(timeElapsed));
					}
				}
				*/

				// Run the actions for reaching this ring
				mNextWaypoint.doActions(new QuestContext(mPlugin, mPlayer, null));

				MessagingUtils.sendActionBarMessage(mPlayer, NamedTextColor.BLUE, true, RaceUtils.msToTimeString(timeElapsed), false);
				mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1.5f);

				if (mRemainingWaypoints.size() == 0) {
					win(timeElapsed);
				} else {
					mNextWaypoint = mRemainingWaypoints.removeFirst();
				}
			}

			animation();

		}
	}

	/*
	 * TODO (low priority)
	 * This creates a lot of new objects every tick - would be nice to optimize
	 */
	private void animation() {
		mFrame++;

		Location rLoc = mNextWaypoint.getPosition().toLocation(mWorld);

		// Ring location faces player
		rLoc.setDirection(mPlayer.getLocation().toVector().subtract(mNextWaypoint.getPosition()));
		int i = 0;
		for (Location loc : RaceUtils.transformPoints(rLoc, RING_SHAPE, rLoc.getYaw(), -rLoc.getPitch(), 0d, mNextWaypoint.getRadius() + 0.5 * Math.cos(Math.toRadians(mFrame * 20)))) {
			loc.subtract(0, 0.5, 0);
			mRingEntities.get(i).teleport(loc);
			i++;
		}
	}

	/*
	 * This should only be called from win() and lose()
	 */
	private void end() {
		mManager.removeRace(mPlayer);
		// Make sure the countdown is stopped, in case someone wants
		// to leave a race during the countdown.
		mCountdownActive = false;

		if (mTimeBar != null) {
			mTimeBar.cancel();
		}
		for (Entity e : mRingEntities) {
			e.remove();
		}
		mPlayer.removeScoreboardTag(RaceManager.PLAYER_RACE_TAG);
	}

	public void lose() {
		if (mLoseActions != null) {
			mLoseActions.doActions(new QuestContext(mPlugin, mPlayer, null));
		}
		mPlayer.teleport(mStopLoc);
		end();
	}

	/*
	 * Called by race manager when cancelling all races
	 */
	public void abort() {
		if (mLoseActions != null) {
			mLoseActions.doActions(new QuestContext(mPlugin, mPlayer, null));
		}
		mPlayer.teleport(mStopLoc);
		if (mTimeBar != null) {
			mTimeBar.cancel();
		}
		for (Entity e : mRingEntities) {
			e.remove();
		}
	}

	public void win(int endTime) {
		end();

		/* Set score on player */
		if (mScoreboard != null) {
			Score score = mScoreboard.getScore(mPlayer.getName());
			if (!score.isScoreSet() || score.getScore() == 0 || endTime < score.getScore()) {
				score.setScore(endTime);

				/* If the RedisSync plugin is also present, update the score in the leaderboard cache */
				if (Bukkit.getServer().getPluginManager().isPluginEnabled("MonumentaRedisSync")) {
					MonumentaRedisSyncAPI.updateLeaderboardAsync(mScoreboard.getName(), mPlayer.getName(), endTime);
				}

				// handle new world record
				if (mWRTime > endTime) {
					String cmdStr = "broadcastcommand tellraw @a [\"\",{\"text\":\"" +
						mPlayer.getName() +
						"\",\"color\":\"blue\"},{\"text\":\" has set a new world record for \",\"color\":\"dark_aqua\"},{\"text\":\"" +
						mName +
						"\",\"color\":\"blue\"},{\"text\":\"\\nNew time: \",\"color\":\"dark_aqua\"},{\"text\":\"" +
						RaceUtils.msToTimeString(endTime) +
						"\",\"color\":\"blue\"}]";
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdStr);
				}
			}
		}

		//TODO: Ring times
		/*
		int pb = (has_ring_times ? ringTimes.get(ringTimes.size() - 1) : possibleRingTimes.get(possibleRingTimes.size() - 1));
		if (!has_ring_times || endTime < pb) { // if time beaten
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
			mPlayer.sendMessage("" + ChatColor.DARK_AQUA + ChatColor.BOLD + "----====----       " + ChatColor.AQUA + ChatColor.BOLD + "Speedruns" + ChatColor.DARK_AQUA + ChatColor.BOLD + "       ----====----\n");
			mPlayer.sendMessage(" " + String.format("%s - %s", "" + ChatColor.AQUA + "Race Recap", " " + ChatColor.YELLOW + mName));
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
				time.doActions(new QuestContext(mPlugin, mPlayer, null));
			}
		}
	}

	private void updateWRTime() {
		if (mScoreboard == null || mScoreboard.getScoreboard() == null) {
			// no scoreboard = statless race
			return;
		}

		Objective finalScoreboard = mScoreboard;

		/* If the RedisSync plugin is also present, update the score in the leaderboard cache */
		if (Bukkit.getServer().getPluginManager().isPluginEnabled("MonumentaRedisSync")) {
			/* Get the lowest value from the redis leaderboard that's not zero */
			Bukkit.getScheduler().runTaskAsynchronously(mPlugin, () -> {
				try {
					Map<String, Integer> values = MonumentaRedisSyncAPI.getLeaderboard(finalScoreboard.getName(), 0, -1, true).get();
					for (Map.Entry<String, Integer> entry : values.entrySet()) {
						// These are already in sorted order - stop at the first non-zero one
						if (entry.getValue() > 0) {
							mWRTime = entry.getValue();
							return;
						}
					}
				} catch (Exception ex) {
					mPlugin.getLogger().severe("Failed to get world record time for leaderboard " + finalScoreboard.getName() + ": " + ex.getMessage());
					ex.printStackTrace();
				}
			});
		} else {
			/* Get the lowest value from the scoreboard that's not zero */
			int top = Integer.MAX_VALUE;
			int score;
			for (String name : finalScoreboard.getScoreboard().getEntries()) {
				score = finalScoreboard.getScore(name).getScore();
				if (score < top && score > 0) {
					top = score;
				}
			}
			mWRTime = top;
		}
	}

	public boolean isRingless() {
		return mRingless;
	}

	public boolean allowsDialogClick() {
		return mAllowDialogClick;
	}

	public boolean allowsCode() {
		return mAllowCode;
	}

	public boolean allowsClickables() {
		return mAllowClickables;
	}

	public boolean allowsNpcInteraction() {
		return mAllowNpcInteraction;
	}

	public boolean isStatless() {
		return !mShowStats;
	}
}
