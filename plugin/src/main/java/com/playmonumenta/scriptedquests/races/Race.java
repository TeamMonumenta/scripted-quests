package com.playmonumenta.scriptedquests.races;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.managers.RaceManager;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.utils.RaceUtils;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import org.bukkit.scoreboard.Scoreboard;
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
	private static final String PLAYER_RACE_SPEED_TAG = "SQRacerSpeed";
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
	private @Nullable ArrayList<Integer> mPBRingTimes;
	private final ArrayList<Integer> mCurrentRingTimes = new ArrayList<>();
	private int mCurrentWaypointIndex = 0;

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
		getPBRingTimes();

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
		if (mPlayer.getScoreboardTags().contains(PLAYER_RACE_SPEED_TAG)) {
			mPlayer.getScoreboardTags().remove(PLAYER_RACE_SPEED_TAG);
			Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
			Objects.requireNonNull(scoreboard.getObjective("Speed")).getScore(mPlayer.getName()).setScore(0);
			Objects.requireNonNull(scoreboard.getObjective("SpeedB")).getScore(mPlayer.getName()).setScore(0);
			mPlayer.getScoreboardTags().add(PLAYER_RACE_SPEED_TAG);
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
		mCurrentWaypointIndex = 0;
		mCurrentRingTimes.clear();

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
				Component timeMessage = Component.text(RaceUtils.msToTimeString(timeElapsed), NamedTextColor.BLUE).decorate(TextDecoration.BOLD);

				int oldTime = (mPBRingTimes != null) ? mPBRingTimes.get(mCurrentWaypointIndex) : 0;
				if (oldTime != 0) {
					int delta = timeElapsed - oldTime;
					NamedTextColor deltaColor = (oldTime == timeElapsed) ? NamedTextColor.GRAY : (delta > 0) ? NamedTextColor.RED : NamedTextColor.GREEN;
					timeMessage = timeMessage.append(Component.text(" %s%s".formatted((delta < 0) ? "-" : "+", RaceUtils.msToTimeString(Math.abs(delta))), deltaColor).decorate(TextDecoration.BOLD));
				}
				mCurrentRingTimes.add(timeElapsed);

				// Run the actions for reaching this ring
				mNextWaypoint.doActions(new QuestContext(mPlugin, mPlayer, null));

				//MessagingUtils.sendActionBarMessage(mPlayer, NamedTextColor.BLUE, true, RaceUtils.msToTimeString(timeElapsed), false);
				mPlayer.sendMessage(timeMessage);
				mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1.5f);

				if (mRemainingWaypoints.size() == 0) {
					win(timeElapsed);
				} else {
					mNextWaypoint = mRemainingWaypoints.removeFirst();
					mCurrentWaypointIndex++;
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
		@Nullable Objective mSpeedScoreboard = null;
		int authorTime = 0;
		if (mScoreboard != null) {
			String newName = mScoreboard.getName() + "-Speed";
			Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
			mSpeedScoreboard = scoreboard.getObjective(newName);
			if (scoreboard.getObjective("AuthorMedal") != null) {
				authorTime = Objects.requireNonNull(scoreboard.getObjective("AuthorMedal")).getScore(mPlayer.getName()).getScore();
			}
		}
		int playerPosition1 = getPlayerPosition(mPlayer, mSpeedScoreboard);
		end();
		int speedScore = 0;
		int speedWR = 0;
		if (mPlayer.getScoreboardTags().contains(PLAYER_RACE_SPEED_TAG)) {
			speedScore = Objects.requireNonNull(mPlayer.getScoreboard().getObjective("Speed")).getScore(mPlayer.getName()).getScore();
			if (endTime > mTimes.get(0).getTime()) {
				mPlayer.sendMessage("" + ChatColor.RED + ChatColor.BOLD + "Since you didn't achieve master time, your time on the Lowest Speed % leaderboard was not updated!");
				mPlayer.getScoreboardTags().remove(PLAYER_RACE_SPEED_TAG);
			}
		}

		if (mScoreboard != null && mScoreboard.getScoreboard() != null) {
			JsonArray pbArray = new JsonArray();
			if (mPBRingTimes == null) {
				mCurrentRingTimes.forEach(pbArray::add);
			} else {
				for (int i = 0; i < mCurrentRingTimes.size(); i++) {
					int pbTime = mPBRingTimes.get(i);
					int currTime = mCurrentRingTimes.get(i);
					int delta = pbTime - currTime;
					if (delta > 0) {
						pbArray.add(currTime);
					} else {
						pbArray.add(pbTime);
					}
				}
			}

			// Replace the pb for this race in the JsonObject map.
			JsonObject pbObject = RaceManager.PLAYER_RACE_RING_PB_TIMES.get(mPlayer.getUniqueId());
			if (pbObject != null) {
				pbObject.add(mScoreboard.getName(), pbArray);
			} else {
				JsonObject newPBObject = new JsonObject();
				newPBObject.add(mScoreboard.getName(), pbArray);
				RaceManager.PLAYER_RACE_RING_PB_TIMES.put(mPlayer.getUniqueId(), newPBObject);
			}
		}

		// display race end info
		//header
		if (mShowStats) {
			mPlayer.sendMessage("" + ChatColor.DARK_AQUA + ChatColor.BOLD + "----====----       " + ChatColor.AQUA + ChatColor.BOLD + "Speedruns" + ChatColor.DARK_AQUA + ChatColor.BOLD + "       ----====----\n");
			String recapMessage = String.format("%s - %s",
				"" + ChatColor.AQUA + "Race Recap",
				" " + ChatColor.YELLOW + mName
			);

			if (mPlayer.getScoreboardTags().contains(PLAYER_RACE_SPEED_TAG)) {
				recapMessage += " " + ChatColor.GREEN + "(Lowest Speed %)";
			}
			mPlayer.sendMessage(" " + recapMessage);
			mPlayer.sendMessage(" ");
		}

		//TODO: World record time first

		if ((!mPlayer.getScoreboardTags().contains(PLAYER_RACE_SPEED_TAG))) {
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
		} else {
			if (mSpeedScoreboard != null) {
				int personalBest = mSpeedScoreboard.getScore(mPlayer.getName()).getScore();
				speedWR = Objects.requireNonNull(mSpeedScoreboard.getScoreboard()).getEntries().stream()
					.map(mSpeedScoreboard::getScore)
					.filter(score -> score.isScoreSet() && score.getScore() > 0)
					.min(Comparator.comparingInt(Score::getScore))
					.orElseThrow(() -> new IllegalStateException("No entries found"))
					.getScore();
				if (!mTimes.isEmpty()) {
					RaceTime masterTime = mTimes.get(0);
					int medalTime = masterTime.getTime();
					mPlayer.sendMessage(String.format("  %s%s      - %22s  | %s %s",
						"" + masterTime.getColor(),
						"" + masterTime.getLabel(),
						"" + RaceUtils.msToTimeString(medalTime),
						"" + ((endTime <= medalTime) ? ("" + masterTime.getColor() + "\u272A") : ("" + ChatColor.GRAY + ChatColor.BOLD + "\u272A")),
						"" + ((endTime <= medalTime) ? ("" + ChatColor.BLUE + ChatColor.BOLD + "( -" + RaceUtils.msToTimeString(medalTime - endTime) + ")") : ("" + ChatColor.RED + ChatColor.BOLD + "( +" + RaceUtils.msToTimeString(endTime - medalTime) + ")"))));

				}
				int timeDifference = speedScore - authorTime;
				String differenceString = (timeDifference != 0)
					? ((timeDifference > 0)
					? "" + ChatColor.RED + ChatColor.BOLD + " (+" + timeDifference + ")"
					: "" + ChatColor.BLUE + ChatColor.BOLD + " (-" + Math.abs(timeDifference) + ")")
					: "";
				mPlayer.sendMessage(String.format("  %s  Author Medal   - %18s  | %s",
					"" + ChatColor.BLUE + ChatColor.BOLD,
					"" + ChatColor.GREEN + ChatColor.BOLD + authorTime,
					differenceString
				));
				mPlayer.sendMessage(String.format("  %s  Your Speed   - %19s",
					"" + ChatColor.BLUE + ChatColor.BOLD,
					"" + ChatColor.GREEN + ChatColor.BOLD + speedScore));
				mPlayer.sendMessage(" ");
				mPlayer.sendMessage(String.format("  %sWorld Record - %16s  | %s %s",
					"" + ChatColor.AQUA + ChatColor.BOLD,
					"" + speedWR,
					"" + ((speedScore <= speedWR) ? ("" + ChatColor.AQUA + ChatColor.BOLD + "\u272A") : ("" + ChatColor.GRAY + ChatColor.BOLD + "\u272A")),
					"" + ((speedWR - speedScore != 0)
						? ((speedScore <= speedWR)
						? ("" + ChatColor.BLUE + ChatColor.BOLD + "( -" + (speedWR - speedScore) + ")")
						: ("" + ChatColor.RED + ChatColor.BOLD + "( +" + (speedScore - speedWR) + ")"))
						: "")
				));
				mPlayer.sendMessage(String.format("  %s  Personal Best - %14s  | %s",
					"" + ChatColor.BLUE + ChatColor.BOLD,
					"" + personalBest,
					"" + ((personalBest - speedScore != 0)
						? ((speedScore <= personalBest)
						? ("" + ChatColor.BLUE + ChatColor.BOLD + "( -" + (personalBest - speedScore) + ")")
						: ("" + ChatColor.RED + ChatColor.BOLD + "( +" + (speedScore - personalBest) + ")"))
						: "")
				));

			}
		}

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


		/* Last thing is to do any actions associated with the race */
		for (RaceTime time : mTimes) {
			if (endTime <= time.getTime()) {
				time.doActions(new QuestContext(mPlugin, mPlayer, null));
			}
		}
		if (mPlayer.getScoreboardTags().contains(PLAYER_RACE_SPEED_TAG)) {
			int playerPosition2 = getPlayerPosition(mPlayer, mSpeedScoreboard);
			if (playerPosition1 != 0 && playerPosition2 != 0) {
				int positionDifference = playerPosition1 - playerPosition2;
				String differenceMessage = positionDifference > 0 ? " | (-" + positionDifference + ")" : "";
				mPlayer.sendMessage(String.format("  %s  Current Position - %12s%d%s%s",
					ChatColor.GOLD + "" + ChatColor.BOLD,
					ChatColor.WHITE + "" + ChatColor.BOLD,
					playerPosition2,
					ordinalSuffix(playerPosition2),
					differenceMessage
				));
			} else {
				mPlayer.sendMessage(ChatColor.RED + "You are not on the leaderboard.");
			}

			if (speedWR > speedScore) {
				String cmdStr = "broadcastcommand tellraw @a [\"\",{\"text\":\"" +
					mPlayer.getName() +
					"\",\"color\":\"blue\"},{\"text\":\" has set a new world record for \",\"color\":\"dark_aqua\"},{\"text\":\"" +
					mName +
					"\",\"color\":\"blue\"},{\"text\":\"\\nNew Lowest Master Speed: \",\"color\":\"dark_aqua\"},{\"text\":\"" +
					speedScore +
					"%\",\"color\":\"blue\"}]";
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdStr);
			}
		}
	}

	private String ordinalSuffix(int position) {
		if (position >= 11 && position <= 13) return "th";
		switch (position % 10) {
			case 1: return "st";
			case 2: return "nd";
			case 3: return "rd";
			default: return "th";
		}
	}

	public int getPlayerPosition(Player mPlayer, Objective mSpeedScoreboard) {
		if (mSpeedScoreboard != null) {
			List<AbstractMap.SimpleEntry<String, Integer>> sortedEntries = mSpeedScoreboard.getScoreboard().getEntries().stream()
				.map(entry -> new AbstractMap.SimpleEntry<>(entry, mSpeedScoreboard.getScore(entry).getScore()))
				.filter(entry -> entry.getValue() != 0)
				.sorted(Map.Entry.comparingByValue())
				.toList();

			for (int i = 0; i < sortedEntries.size(); i++) {
				if (sortedEntries.get(i).getKey().equals(mPlayer.getName())) {
					return i + 1;
				}
			}
		}
		return -1;
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

	private void getPBRingTimes() {
		if (mRingless || mScoreboard == null || mScoreboard.getScoreboard() == null) {
			return;
		}

		JsonObject ringPBsObject = RaceManager.PLAYER_RACE_RING_PB_TIMES.get(mPlayer.getUniqueId());

		if (ringPBsObject == null) {
			return;
		}

		JsonElement raceRingPBElement = ringPBsObject.get(mScoreboard.getName());

		if (raceRingPBElement == null) {
			return;
		}

		JsonArray raceRingPBArray = raceRingPBElement.getAsJsonArray();
		ArrayList<Integer> ringPBs = new ArrayList<>();
		raceRingPBArray.forEach(element -> ringPBs.add(element.getAsInt()));
		mPBRingTimes = ringPBs;
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
