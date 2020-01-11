package com.playmonumenta.scriptedquests.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.zones.ZoneChangeEvent;
import com.playmonumenta.scriptedquests.zones.zone.BaseZone;
import com.playmonumenta.scriptedquests.zones.zone.Zone;
import com.playmonumenta.scriptedquests.zones.zone.ZoneFragment;
import com.playmonumenta.scriptedquests.zones.zonetree.BaseZoneTree;

public abstract class ZoneManager {
	static BukkitRunnable playerTracker = null;

	private Plugin mPlugin;
	private BaseZoneTree mZoneTree;
	private Map<Player, ZoneFragment> lastPlayerZoneFragment = new HashMap<Player, ZoneFragment>();

	public ZoneManager(Plugin plugin, ArrayList<Zone> zones) throws Exception {
		if (zones == null) {
			throw new Exception("zones may not be null.");
		}

		// TODO Load zones from files, not external code. Must be sorted by priority first.
		mPlugin = plugin;

		// Split the zones into non-overlapping fragments
		removeOverlaps(zones);
		// TODO Defragment to reduce fragment count (approx 2-3x on average)
		// Create list of all zone fragments
		ArrayList<ZoneFragment> zoneFragments = new ArrayList<ZoneFragment>();
		for (Zone zone : zones) {
			zoneFragments.addAll(zone.zoneFragments());
		}

		mZoneTree = BaseZoneTree.CreateZoneTree(zoneFragments);

		// Make sure only one player tracker runs at a time
		if (playerTracker != null && !playerTracker.isCancelled()) {
			playerTracker.cancel();
		}

		// Start a new task to track players changing zones
		playerTracker = new BukkitRunnable() {
			@Override
			public void run() {
				for (Player player : Bukkit.getOnlinePlayers()) {
					Location playerLocation = player.getLocation();

					ZoneFragment lastZoneFragment = lastPlayerZoneFragment.get(player);
					if (lastZoneFragment != null && lastZoneFragment.within(playerLocation)) {
						// Player has not left their previous zone fragment; no zone change.
						continue;
					}
					Zone lastZone = null;
					if (lastZoneFragment != null) {
						lastZone = lastZoneFragment.parent();
					}

					ZoneFragment currentZoneFragment = getZoneFragment(playerLocation);
					Zone currentZone = null;
					if (currentZoneFragment != null) {
						currentZone = currentZoneFragment.parent();
					}

					// We've already confirmed the player changed zone fragments; null is valid.
					lastPlayerZoneFragment.put(player, currentZoneFragment);

					// Zones and ZoneFragments are not cloned after the manager is instantiated; == is valid.
					if (currentZone == lastZone) {
						continue;
					}

					// Zones changed, send an event.
					ZoneChangeEvent event = new ZoneChangeEvent(player, lastZone, currentZone);
					Bukkit.getPluginManager().callEvent(event);
				}
			}
		};

		playerTracker.runTaskTimer(plugin, 0, 5);
	}

	/*
	 * For a given location, return the fragment that contains it.
	 * Returns null if no fragment overlaps it.
	 */
	public ZoneFragment getZoneFragment(Vector loc) {
		return mZoneTree.getZoneFragment(loc);
	}

	public ZoneFragment getZoneFragment(Location loc) {
		return mZoneTree.getZoneFragment(loc);
	}

	/*
	 * For a given location, return the zone that contains it.
	 * Returns null if no zone overlaps it.
	 */
	public Zone getZon(Vector loc) {
		return mZoneTree.getZone(loc);
	}

	public Zone getZone(Location loc) {
		return mZoneTree.getZone(loc);
	}

	private void removeOverlaps(ArrayList<Zone> zones) throws Exception {
		for (int i = 0; i < zones.size(); i++) {
			Zone outer = zones.get(i);
			for (Zone inner : zones.subList(i + 1, zones.size())) {
				BaseZone overlap = outer.overlappingZone(inner);
				if (overlap == null) {
					continue;
				}
				inner.splitByOverlap(overlap);
			}
		}
	}
}
