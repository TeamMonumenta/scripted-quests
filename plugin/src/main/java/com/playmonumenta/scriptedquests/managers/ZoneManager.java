package com.playmonumenta.scriptedquests.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import com.playmonumenta.scriptedquests.zones.ZoneChangeEvent;
import com.playmonumenta.scriptedquests.zones.ZonePropertyChangeEvent;
import com.playmonumenta.scriptedquests.zones.ZoneLayer;
import com.playmonumenta.scriptedquests.zones.zone.BaseZone;
import com.playmonumenta.scriptedquests.zones.zone.Zone;
import com.playmonumenta.scriptedquests.zones.zone.ZoneFragment;
import com.playmonumenta.scriptedquests.zones.zonetree.BaseZoneTree;

public class ZoneManager {
	static BukkitRunnable playerTracker = null;

	private HashMap<String, ZoneLayer> mLayers = new HashMap<String, ZoneLayer>();
	private BaseZoneTree mZoneTree = null;
	private Map<Player, ZoneFragment> lastPlayerZoneFragment = new HashMap<Player, ZoneFragment>();

	/*
	 * If sender is non-null, it will be sent debugging information
	 *
	 * TODO This can easily take several seconds to run, especially once defragmenting zones is possible.
	 *      However, aside from when it first starts up, this is quite fast. Reloading after the first
	 *      startup could even allow for an async load, only pausing long enough to swap the generated tree.
	 *      If that's not good enough, an unbalanced tree is even faster to generate without too much
	 *      slowdown, and the ZoneLayer class could be modified to handle determining which zones have
	 *      priority if startup time NEEDS to be near-instant in exchange for slower clock speeds while the
	 *      tree is loading. We have options. We will probably need some of them.
	 */
	public void reload(Plugin plugin, CommandSender sender) {
		for (ZoneLayer layer : mLayers.values()) {
			layer.invalidate();
		}
		mLayers.clear();

		QuestUtils.loadScriptedQuests(plugin, "zone_layers", sender, (object) -> {
			// Load this file into a ZoneLayer object
			ZoneLayer layer = new ZoneLayer(object);
			String layerName = layer.getName();

			if (mLayers.containsKey(layerName)) {
				throw new Exception("'" + layerName + "' already exists!");
			}

			mLayers.put(layerName, layer);

			return layerName + ":" + Integer.toString(layer.getZones().size());
		});

		// Create list of zones
		ArrayList<Zone> zones = new ArrayList<Zone>();
		for (ZoneLayer layer : mLayers.values()) {
			zones.addAll(layer.getZones());
		}

		// Split the zones into non-overlapping fragments. This could take a little time.
		mergeOverlaps(zones);

		// TODO Defragment to reduce fragment count (approx 2-3x on average). This takes a long time.

		// Create list of all zone fragments.
		ArrayList<ZoneFragment> zoneFragments = new ArrayList<ZoneFragment>();
		for (Zone zone : zones) {
			zoneFragments.addAll(zone.getZoneFragments());
		}

		// Create the new tree. This can take a long time!
		BaseZoneTree newTree = BaseZoneTree.CreateZoneTree(zoneFragments);

		// Make sure only one player tracker runs at a time.
		if (playerTracker != null && !playerTracker.isCancelled()) {
			playerTracker.cancel();
		}

		// Swap the tree out; this is really fast!
		if (mZoneTree != null) {
			mZoneTree.invalidate();
		}
		mZoneTree = newTree;

		// Start a new task to track players changing zones
		playerTracker = new BukkitRunnable() {
			@Override
			public void run() {
				for (Player player : Bukkit.getOnlinePlayers()) {
					Location playerLocation = player.getLocation();
					Vector playerVector = playerLocation.toVector();

					ZoneFragment lastZoneFragment = lastPlayerZoneFragment.get(player);
					if (lastZoneFragment != null && lastZoneFragment.within(playerVector)) {
						// Player has not left their previous zone fragment; no zone change.
						// Note that reloading invalidates previous zones, causing within() to
						// return false regardless of if the player would have been within the fragment.
						continue;
					}

					ZoneFragment currentZoneFragment = getZoneFragment(playerVector);
					if (lastZoneFragment == null && currentZoneFragment == null) {
						// Player was not in a previous zone fragment, and isn't in one now; no zone change.
						continue;
					}

					HashMap<String, Zone> lastZones = new HashMap<String, Zone>();
					if (lastZoneFragment != null) {
						lastZones = lastZoneFragment.getParents();
					}

					HashMap<String, Zone> currentZones = new HashMap<String, Zone>();
					if (currentZoneFragment != null) {
						currentZones = currentZoneFragment.getParents();
					}

					// We've already confirmed the player changed zone fragments; null is valid.
					lastPlayerZoneFragment.put(player, currentZoneFragment);

					if (currentZones == lastZones) {
						// If the zones are identical between both fragments, skip ahead.
						// Zones and ZoneFragments are not cloned after the manager is instantiated; == is valid.
						continue;
					}

					// Zones changed, send an event for each layer.
					LinkedHashSet<String> mentionedLayerNames = new LinkedHashSet<String>();
					mentionedLayerNames.addAll(lastZones.keySet());
					mentionedLayerNames.addAll(currentZones.keySet());
					for (String layerName : mentionedLayerNames) {
						// Null zones are valid - indicates no zone.
						Zone lastZone = lastZones.get(layerName);
						Zone currentZone = currentZones.get(layerName);

						ZoneChangeEvent zoneEvent = new ZoneChangeEvent(player, layerName, lastZone, currentZone);
						Bukkit.getPluginManager().callEvent(zoneEvent);

						LinkedHashSet<String> lastProperties;
						if (lastZone == null) {
							lastProperties = new LinkedHashSet<String>();
						} else {
							lastProperties = lastZone.getProperties();
						}

						LinkedHashSet<String> currentProperties;
						if (currentZone == null) {
							currentProperties = new LinkedHashSet<String>();
						} else {
							currentProperties = currentZone.getProperties();
						}

						LinkedHashSet<String> removedProperties = new LinkedHashSet<String>();
						removedProperties.addAll(lastProperties);
						removedProperties.removeAll(currentProperties);
						for (String property : removedProperties) {
							ZonePropertyChangeEvent event;
							event = new ZonePropertyChangeEvent(player, layerName, "!" + property);
							Bukkit.getPluginManager().callEvent(event);
						}

						LinkedHashSet<String> addedProperties = new LinkedHashSet<String>();
						addedProperties.addAll(currentProperties);
						addedProperties.removeAll(lastProperties);
						for (String property : addedProperties) {
							ZonePropertyChangeEvent event;
							event = new ZonePropertyChangeEvent(player, layerName, property);
							Bukkit.getPluginManager().callEvent(event);
						}
					}
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
		if (loc == null) {
			return null;
		}

		return mZoneTree.getZoneFragment(loc.toVector());
	}

	// For a given location, return the zones that contain it.
	public HashMap<String, Zone> getZones(Vector loc) {
		return mZoneTree.getZones(loc);
	}

	public HashMap<String, Zone> getZones(Location loc) {
		if (loc == null) {
			return null;
		}

		return mZoneTree.getZones(loc.toVector());
	}

	/*
	 * For a given layer and location, return the zone that
	 * contains it. Returns null if no zone overlaps it.
	 */
	public Zone getZone(String layer, Vector loc) {
		return mZoneTree.getZone(layer, loc);
	}

	public Zone getZone(String layer, Location loc) {
		if (loc == null) {
			return null;
		}

		return mZoneTree.getZone(layer, loc.toVector());
	}

	private void mergeOverlaps(ArrayList<Zone> zones) {
		for (int i = 0; i < zones.size(); i++) {
			Zone outer = zones.get(i);
			for (Zone inner : zones.subList(i + 1, zones.size())) {
				BaseZone overlap = outer.overlappingZone(inner);
				if (overlap == null) {
					continue;
				}
				outer.splitByOverlap(overlap, inner);
				inner.splitByOverlap(overlap);
			}
		}
	}
}
