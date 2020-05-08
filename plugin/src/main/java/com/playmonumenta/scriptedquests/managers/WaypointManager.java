package com.playmonumenta.scriptedquests.managers;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.scriptedquests.Plugin;

public class WaypointManager {
	/* Number of ticks between re-checking waypoint progress */
	private static final int WAYPOINT_DELAY = 20;
	/* Threshold of movement relative to moving average which counts as moving */
	private static final double WAYPOINT_PLAYER_MOVE_THRESH = 1.5;
	/* Radius of the helix animation at the destination */
	private static final double WAYPOINT_DEST_ANIM_RADIUS = 2;
	/* Min/max distances the player must be from the goal to play any animations at all */
	private static final double WAYPOINT_SWITCH_OFFSET = 7;
	private static final double WAYPOINT_FINAL_ANIM_MIN_DIST = 6;
	private static final double WAYPOINT_DEST_ANIM_MAX_DIST = 48;
	/* Minimum distance the player must be from the goal to play the fast/slow beam animation */
	private static final double WAYPOINT_BEAM_ANIM_MIN_DIST = 7;
	/* Number of ticks between updates to the slow moving beam */
	private static final int WAYPOINT_SLOW_BEAM_TICK_PERIOD = 5;
	/* Number of total ticks the slow moving beam can be alive for */
	private static final int WAYPOINT_SLOW_BEAM_MAX_TICKS = 140;

	private static final Random RAND = new Random();

	private final Plugin mPlugin;
	private final Map<Player, List<Location>> mPlayers = new LinkedHashMap<Player, List<Location>>();
	private final Map<Player, Location> mPlayerAverageLocs = new LinkedHashMap<Player, Location>();
	private BukkitRunnable mRunnable = null;

	private static double distance2D(Location loc1, Location loc2) {
		return Math.sqrt(Math.pow(loc1.getX() - loc2.getX(), 2) + Math.pow(loc1.getZ() - loc2.getZ(), 2));
	}

	private static Vector getDirectionTo(Location to, Location from) {
		Vector vFrom = from.toVector();
		Vector vTo = to.toVector();
		return vTo.subtract(vFrom).normalize();
	}

	private static Location getNextPoint(Player player, List<Location> waypoints) {
		Location playerLoc = player.getLocation();
		Location prevLoc = null;
		Location closestLoc = null;
		double closestDist = 99999999999999999d;

		/*
		 * Iterate the list to determine the closest-next waypoint
		 *
		 * This is tricky - because the closest waypoint might be the last waypoint
		 * the player visited. So if the distance to the "next" waypoint is less than the distance between
		 * two waypoints, use the next one instead even though it's further away.
		 */
		for (Location loc : waypoints) {
			double dist = distance2D(loc, playerLoc);

			if (closestLoc == null) {
				/* First entry - it's the closest so far */
				closestLoc = loc;
				closestDist = dist;
				prevLoc = loc;
				continue;
			}

			if (dist < closestDist || dist < distance2D(prevLoc, loc) + WAYPOINT_SWITCH_OFFSET) {
				/* This is either the closest location OR it's closer than the distance between this and the last waypoint */
				closestLoc = loc;
				closestDist = dist;
			}

			prevLoc = loc;
		}

		if (closestLoc.getY() < 0) {
			// For backwards compatibility, some waypoints don't have a real y coordinate - so they use the player's y coordinate
			closestLoc = closestLoc.clone();
			closestLoc.setY(playerLoc.getY());
		}
		return closestLoc.clone();
	}

	public WaypointManager(Plugin plugin) {
		mPlugin = plugin;
	}

	/*
	private void spawnLocationParticles(Player player,
	*/

	private void ensureTaskIsRunning() {
		if ((mRunnable != null && !mRunnable.isCancelled()) || mPlayers.isEmpty()) {
			// Already running or no work to do - do nothing
			return;
		}

		mRunnable = new BukkitRunnable() {
			double mHelixStart = 0;

			@Override
			public void run() {
				if (mPlayers.isEmpty()) {
					// Nothing currently to do - cancel the task
					this.cancel();
					return;
				}

				Iterator<Map.Entry<Player, List<Location>>> iter = mPlayers.entrySet().iterator();

				while (iter.hasNext()) {
					Map.Entry<Player, List<Location>> entry = iter.next();
					Player player = entry.getKey();
					List<Location> waypoints = entry.getValue();

					if (!player.isValid() || player.isDead() || !player.isOnline()) {
						iter.remove();
						continue;
					}

					Location targetLoc = getNextPoint(player, waypoints);
					boolean isLast = targetLoc.equals(waypoints.get(waypoints.size() - 1));
					player.setCompassTarget(targetLoc);

					Location playerLoc = player.getLocation();
					double targetDist = playerLoc.distance(targetLoc);

					if (isLast && targetDist < WAYPOINT_FINAL_ANIM_MIN_DIST) {
						iter.remove();
						continue;
					}

					if (player.getScoreboardTags().contains("noCompassParticles")) {
						continue;
					}

					// Spawn slow-moving helix at the target location if it is close enough to be seen
					if (isLast && targetDist < WAYPOINT_DEST_ANIM_MAX_DIST) {
						new BukkitRunnable() {
							double mY = -1.0;
							double mTheta = mHelixStart;

							public void run() {
								Vector offset = new Vector(WAYPOINT_DEST_ANIM_RADIUS * Math.cos(mTheta),
														   mY,
														   WAYPOINT_DEST_ANIM_RADIUS * Math.sin(mTheta));
								player.spawnParticle(Particle.SPELL_INSTANT, targetLoc.clone().add(offset), 1, 0.01, 0.01, 0.01, 0);

								mY += 0.1;
								mTheta += Math.PI / 10;

								double targetDist = player.getLocation().distance(targetLoc);
								if (mY >= 4.0 || targetDist < WAYPOINT_FINAL_ANIM_MIN_DIST || targetDist > WAYPOINT_DEST_ANIM_MAX_DIST) {
									this.cancel();
								}
							}
						}.runTaskTimer(mPlugin, 0, 2);
						mHelixStart += Math.PI / 4;
					}

					/* Rolling average player location - 19 parts previous location, 1 part new location */
					Location averageLoc = mPlayerAverageLocs.get(player);
					if (averageLoc == null) {
						averageLoc = playerLoc.clone();
					}
					averageLoc = averageLoc.add(playerLoc).multiply(0.5d);
					mPlayerAverageLocs.put(player, averageLoc);

					/* Create particles that lead to the destination */
					if (targetDist > WAYPOINT_BEAM_ANIM_MIN_DIST) {
						if (playerLoc.distance(averageLoc) > WAYPOINT_PLAYER_MOVE_THRESH) {
							/* Player is actively moving - need to use fast particles to keep up with them */
							new BukkitRunnable() {
								int mTicks = 0;

								@Override
								public void run() {
									mTicks++;
									Location particleLoc = player.getLocation().add(0, 0.2, 0);
									Vector dir = getDirectionTo(targetLoc.clone().add(0, 0.2, 0), particleLoc);
									particleLoc.add(dir.multiply(8));
									player.spawnParticle(Particle.VILLAGER_HAPPY, particleLoc, 1, 0.1, 0.1, 0.1, 1);
									if (mTicks >= WAYPOINT_DELAY / 3) {
										this.cancel();
									}
								}
							}.runTaskTimer(mPlugin, 0, 3);
						} else {
							/* Player is basically standing still - more detailed animation is possible */
							Location particleLoc = playerLoc.add(0, 0.5, 0);
							Vector dir = getDirectionTo(targetLoc.clone().add(0, 0.5, 0), particleLoc);
							dir.multiply(0.6);
							new BukkitRunnable() {
								Location mPlayerStartLoc = playerLoc.clone();
								int mTicks = 0;

								@Override
								public void run() {
									mTicks += WAYPOINT_SLOW_BEAM_TICK_PERIOD;
									particleLoc.add(dir);
									if (RAND.nextFloat() > 0.5) {
										player.spawnParticle(Particle.VILLAGER_HAPPY, particleLoc, 1, 0.1, 0.1, 0.1, 1);
									}
									// Stop this animated beam when it has lingered too long, reaches the target, or the player moves
									if (mTicks >= WAYPOINT_SLOW_BEAM_MAX_TICKS
										|| particleLoc.distance(targetLoc) < 1.5
										|| player.getLocation().distance(mPlayerStartLoc) > WAYPOINT_PLAYER_MOVE_THRESH) {
										this.cancel();
									}
								}
							}.runTaskTimer(mPlugin, 0, WAYPOINT_SLOW_BEAM_TICK_PERIOD);
						}
					}
				}
			}
		};
		mRunnable.runTaskTimer(mPlugin, 0, WAYPOINT_DELAY);
	}

	public void cancelAll() {
		mPlayers.clear();
		if (mRunnable != null && !mRunnable.isCancelled()) {
			mRunnable.cancel();
		}
	}

	public void setWaypoint(Player player, List<Location> waypoints) {
		if (waypoints == null || waypoints.isEmpty()) {
			mPlayers.remove(player);
			return;
		}
		mPlayers.put(player, waypoints);
		ensureTaskIsRunning();
	}
}
