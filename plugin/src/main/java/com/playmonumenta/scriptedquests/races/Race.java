package com.playmonumenta.scriptedquests.races;

import com.playmonumenta.scriptedquests.managers.RaceManager;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestActions;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.RaceUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scoreboard.Objective;
import org.bukkit.Sound;
import org.bukkit.util.Vector;
import org.bukkit.World;

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
	private int mTicks;
	private int mFrame;

	public Race(Plugin plugin, RaceManager manager, Player player, String name, String label,
	            Objective scoreboard, boolean showStats, Location start,
	            List<RaceWaypoint> waypoints, List<RaceTime> times, QuestActions loseActions) {
		mPlugin = plugin;
		mManager = manager;
		mPlayer = player;
		mName = name;
		mLabel = label;
		mScoreboard = scoreboard;
		mShowStats = showStats;
		mStart = start;
		mWaypoints = waypoints;
		mTimes = times;
		mLoseActions = loseActions;

		mWorld = player.getWorld();
		mStopLoc = player.getLocation();

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
		mStartTime = System.currentTimeMillis();
		mMaxTime = mTimes.get(mTimes.size() - 1).getTime();

		// Copy the waypoints to something local to work with
		mRemainingWaypoints = new ArrayDeque<RaceWaypoint>(mWaypoints);
		mNextWaypoint = mRemainingWaypoints.removeFirst();
	}

	public void tick() {
		double distance = mPlayer.getEyeLocation().toVector().distance(mNextWaypoint.getPosition());

		// Time remaining display
		int timeElapsed = (int)(System.currentTimeMillis() - mStartTime);
		if (timeElapsed > mMaxTime) {
			mPlayer.sendMessage("" + ChatColor.RED + ChatColor.BOLD + "You ran out of time!");
			lose();
		}
		//TODO
		//timeBar.update(mPlayer, timeElapsed, medTimes);

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

		Location ploc = mPlayer.getLocation();
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

		//TODO
		//timeBar.bar.setVisible(false);
		for (Entity e : mRingEntities) {
			e.remove();
		}
	}

	public void lose() {
		if (mLoseActions != null) {
			mLoseActions.doActions(mPlugin, mPlayer, null);
		}
		mPlayer.teleport(mStopLoc);
		end();
	}

	private void win(int endTime) {
		end();

		//TODO: Record time here

		if (!mShowStats) {
			return;
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
		mPlayer.sendMessage("" + ChatColor.DARK_AQUA + ChatColor.BOLD +   "----====----       " + ChatColor.AQUA + ChatColor.BOLD + "Speedruns" + ChatColor.DARK_AQUA  + ChatColor.BOLD + "       ----====----\n");
		mPlayer.sendMessage(" " +  String.format("%s - %s", "" + ChatColor.AQUA + "Race Recap", " " + ChatColor.YELLOW + mName));
		mPlayer.sendMessage(" ");

		//TODO: World record time first
		/*
		mPlayer.sendMessage(String.format("  %sWorld Record - %16s  | %s %s",
										  "" + ChatColor.AQUA + ChatColor.BOLD,
										  "" + RaceUtils.msToTimeString(wrTime),
										  "" + ((endTime <= wrTime) ? ("" + ChatColor.AQUA + ChatColor.BOLD + "\u272A") : ("" + ChatColor.GRAY + ChatColor.BOLD + "\u272A")),
										  "" + ((endTime <= wrTime) ? ("" + ChatColor.BLUE + ChatColor.BOLD + "( -" + RaceUtils.msToTimeString(wrTime - endTime) + ")") : ("" + ChatColor.RED + ChatColor.BOLD + "( +" + RaceUtils.msToTimeString(endTime - wrTime) + ")"))));
										  */

		int bestMedalTime = Integer.MAX_VALUE;
		String bestMedalColor = null;
		for (RaceTime time : mTimes) {
			int medalTime = time.getTime();
			mPlayer.sendMessage(String.format("  %s   %s      - %16s  | %s %s",
											  "" + time.getColor(),
											  "" + time.getLabel(),
											  "" + RaceUtils.msToTimeString(medalTime),
											  "" + ((endTime <= medalTime) ? ("" + time.getColor() + "\u272A") : ("" + ChatColor.GRAY + ChatColor.BOLD + "\u272A")),
											  "" + ((endTime <= medalTime) ? ("" + ChatColor.BLUE + ChatColor.BOLD + "( -" + RaceUtils.msToTimeString(medalTime - endTime) + ")") : ("" + ChatColor.RED + ChatColor.BOLD + "( +" + RaceUtils.msToTimeString(endTime - medalTime) + ")"))));

			if (endTime <= medalTime) {
				time.doActions(mPlugin, mPlayer);
				if (medalTime < bestMedalTime) {
					bestMedalTime = medalTime;
					bestMedalColor = time.getColor();
				}
			}
		}

		mPlayer.sendMessage(" ");

		if (mScoreboard != null) {
			int personalBest = mScoreboard.getScore(mPlayer.getName()).getScore();
			mPlayer.sendMessage(String.format("  %s Personal Best - %16s  | %s",
											  "" + ChatColor.BLUE + ChatColor.BOLD,
											  "" + RaceUtils.msToTimeString(personalBest),
											  "" + ((endTime <= personalBest) ? ("" + ChatColor.BLUE + ChatColor.BOLD + "( -" + RaceUtils.msToTimeString(personalBest - endTime) + ")") : ("" + ChatColor.RED + ChatColor.BOLD + "( +" + RaceUtils.msToTimeString(endTime - personalBest) + ")"))));
		}

		mPlayer.sendMessage(String.format("  %s  Your Time   - %16s",
										  "" + ChatColor.BLUE + ChatColor.BOLD,
										  "" + ChatColor.ITALIC + bestMedalColor + RaceUtils.msToTimeString(endTime)));
	}
}
