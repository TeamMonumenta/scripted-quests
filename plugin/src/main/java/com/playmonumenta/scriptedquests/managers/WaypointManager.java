package com.playmonumenta.scriptedquests.managers;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
	private static final double WAYPOINT_PLAYER_MOVE_THRESH = 2;
	/* Radius of the helix animation at the destination */
	private static final double WAYPOINT_DEST_ANIM_RADIUS = 2;
	/* Min/max distances the player must be from the goal to play any animations at all */
	private static final double WAYPOINT_DEST_ANIM_MIN_DIST = 5;
	private static final double WAYPOINT_DEST_ANIM_MAX_DIST = 48;
	/* Minimum distance the player must be from the goal to play the fast/slow beam animation */
	private static final double WAYPOINT_BEAM_ANIM_MIN_DIST = 16;
	/* Number of ticks between updates to the slow moving beam */
	private static final int WAYPOINT_SLOW_BEAM_TICK_PERIOD = 5;
	/* Number of total ticks the slow moving beam can be alive for */
	private static final int WAYPOINT_SLOW_BEAM_MAX_TICKS = 140;

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

			if (dist < closestDist || dist < distance2D(prevLoc, loc) + 5) {
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
		return closestLoc;
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
			double helixStart = 0;

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
					player.setCompassTarget(targetLoc);

					Location playerLoc = player.getLocation();
					double targetDist = playerLoc.distance(targetLoc);

					if (targetDist < WAYPOINT_DEST_ANIM_MIN_DIST) {
						iter.remove();
						continue;
					}

					// Spawn slow-moving helix at the target location if it is close enough to be seen
					if (targetDist < WAYPOINT_DEST_ANIM_MAX_DIST) {
						new BukkitRunnable() {
							double y = -1.0;
							double theta = helixStart;

							public void run() {
								Vector offset = new Vector(WAYPOINT_DEST_ANIM_RADIUS * Math.cos(theta),
														   y,
														   WAYPOINT_DEST_ANIM_RADIUS * Math.sin(theta));
								player.spawnParticle(Particle.SPELL_INSTANT, targetLoc.add(offset), 1, 0.01, 0.01, 0.01, 0);

								y += 0.1;
								theta += Math.PI / 10;

								double targetDist = player.getLocation().distance(targetLoc);
								if (y >= 4.0 || targetDist < WAYPOINT_DEST_ANIM_MIN_DIST || targetDist > WAYPOINT_DEST_ANIM_MAX_DIST) {
									this.cancel();
								}
							}
						}.runTaskTimer(mPlugin, 0, 2);
						helixStart += Math.PI / 4;
					}

					/* Rolling average player location - 19 parts previous location, 1 part new location */
					Location averageLoc = mPlayerAverageLocs.get(player);
					if (averageLoc == null) {
						averageLoc = playerLoc.clone();
					}
					averageLoc = averageLoc.multiply(19.0d).add(playerLoc).multiply(0.05d);

					/* Create particles that lead to the destination */
					if (playerLoc.distance(averageLoc) > WAYPOINT_PLAYER_MOVE_THRESH && targetDist > WAYPOINT_BEAM_ANIM_MIN_DIST) {
						/* Player is actively moving - need to use fast particles to keep up with them */
						new BukkitRunnable() {
							int t = 0;

							@Override
							public void run() {
								t++;
								Location particleLoc = playerLoc.add(0, 0.5, 0);
								Vector dir = getDirectionTo(targetLoc.clone().add(0, 0.5, 0), particleLoc);
								particleLoc.add(dir.multiply(6));
								player.spawnParticle(Particle.VILLAGER_HAPPY, particleLoc, 2, 0.1, 0.1, 0.1, 0);
								if (t >= WAYPOINT_DELAY) {
									this.cancel();
								}
							}
						}.runTaskTimer(mPlugin, 0, 1);
					} else {
						/* Player is basically standing still - more detailed animation is possible */
						Location particleLoc = playerLoc.add(0, 0.5, 0);
						Vector dir = getDirectionTo(targetLoc.clone().add(0, 0.5, 0), particleLoc);
						dir.multiply(0.6);
						new BukkitRunnable() {
							Location playerStartLoc = playerLoc.clone();
							int t = 0;

							@Override
							public void run() {
								t += WAYPOINT_SLOW_BEAM_TICK_PERIOD;
								particleLoc.add(dir);
								player.spawnParticle(Particle.VILLAGER_HAPPY, particleLoc, 2, 0.1, 0.1, 0.1, 0);
								// Stop this animated beam when it has lingered too long, reaches the target, or the player moves
								if (t >= WAYPOINT_SLOW_BEAM_MAX_TICKS
								    || particleLoc.distance(targetLoc) < 1.5
									|| player.getLocation().distance(playerStartLoc) > WAYPOINT_PLAYER_MOVE_THRESH) {
									this.cancel();
								}
							}
						}.runTaskTimer(mPlugin, 0, WAYPOINT_SLOW_BEAM_TICK_PERIOD);
					}
				}
			}
		};
		mRunnable.runTaskTimer(mPlugin, 0, WAYPOINT_DELAY);
	}

	public void cancelAll() {
		mPlayers.clear();
		mRunnable.cancel();
	}

	public void setWaypoint(Player player, List<Location> waypoints) {
		if (waypoints == null || waypoints.isEmpty()) {
			mPlayers.remove(player);
		}
		if (!player.getScoreboardTags().contains("noCompassParticles")) {
			// This player has compass particles enabled
			mPlayers.put(player, waypoints);
		}
		ensureTaskIsRunning();
	}
}
