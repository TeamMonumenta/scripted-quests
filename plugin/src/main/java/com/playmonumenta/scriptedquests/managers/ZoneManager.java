package com.playmonumenta.scriptedquests.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import com.playmonumenta.scriptedquests.zones.ZoneChangeEvent;
import com.playmonumenta.scriptedquests.zones.ZoneLayer;
import com.playmonumenta.scriptedquests.zones.ZonePropertyChangeEvent;
import com.playmonumenta.scriptedquests.zones.zone.BaseZone;
import com.playmonumenta.scriptedquests.zones.zone.Zone;
import com.playmonumenta.scriptedquests.zones.zone.ZoneFragment;
import com.playmonumenta.scriptedquests.zones.zonetree.BaseZoneTree;

public class ZoneManager {
	/* Currently unused - the Zone Manager doesn't need to store any extra data on the Zone objects themselves */
	private static class T { }

	private Plugin mPlugin;
	static BukkitRunnable mPlayerTracker = null;

	private Map<String, ZoneLayer<T>> mLayers = new HashMap<String, ZoneLayer<T>>();
	private Map<String, ZoneLayer<T>> mPluginLayers = new HashMap<String, ZoneLayer<T>>();
	private BaseZoneTree<T> mZoneTree = null;
	private Map<Player, ZoneFragment<T>> mLastPlayerZoneFragment = new HashMap<Player, ZoneFragment<T>>();
	private Map<Player, Map<String, Zone<T>>> mLastPlayerZones = new HashMap<Player, Map<String, Zone<T>>>();

	public ZoneManager(Plugin plugin) {
		mPlugin = plugin;
	}

	/************************************************************************************
	 * Start of methods for use with external plugins:
	 ************************************************************************************/

	/*
	 * Register a ZoneLayer from an external plugin.
	 *
	 * Returns true on success, false on failure.
	 */
	public boolean registerPluginZoneLayer(ZoneLayer<T> layer) {
		if (layer == null) {
			return false;
		}

		String layerName = layer.getName();

		if (mPluginLayers.containsKey(layerName)) {
			return false;
		}

		mPluginLayers.put(layerName, layer);
		reload(mPlugin, null);
		return true;
	}

	/*
	 * Unregister a ZoneLayer from an external plugin.
	 *
	 * Be sure to call invalidate() on the old layer before discarding to ease garbage collection.
	 *
	 * Returns true on success, false on failure.
	 */
	public boolean unregisterPluginZoneLayer(String layerName) {
		if (!mPluginLayers.containsKey(layerName)) {
			return false;
		}

		mPluginLayers.remove(layerName);
		reload(mPlugin, null);
		return true;
	}

	/*
	 * Replace a ZoneLayer from an external plugin.
	 *
	 * Be sure to call invalidate() on the old layer before discarding to ease garbage collection.
	 *
	 * Returns true on success, false on failure.
	 */
	public boolean replacePluginZoneLayer(ZoneLayer<T> layer) {
		if (layer == null) {
			return false;
		}

		String layerName = layer.getName();

		mPluginLayers.put(layerName, layer);
		reload(mPlugin, null);
		return true;
	}

	/*
	 * For a given location, return the fragment that contains it.
	 * Returns null if no fragment overlaps it.
	 *
	 * If fallback zone lookup is enabled, this should be avoided.
	 */
	public ZoneFragment<T> getZoneFragment(Vector loc) {
		return mZoneTree.getZoneFragment(loc);
	}

	public ZoneFragment<T> getZoneFragment(Location loc) {
		if (loc == null) {
			return null;
		}

		return mZoneTree.getZoneFragment(loc.toVector());
	}

	// For a given location, return the zones that contain it.
	public Map<String, Zone<T>> getZones(Vector loc) {
		return getZonesInternal(loc, mPlugin.mFallbackZoneLookup);
	}

	public Map<String, Zone<T>> getZones(Location loc) {
		if (loc == null) {
			return new HashMap<String, Zone<T>>();
		}

		return mZoneTree.getZones(loc.toVector());
	}

	/*
	 * For a given layer and location, return the zone that
	 * contains it. Returns null if no zone overlaps it.
	 */
	public Zone<T> getZone(Vector loc, String layer) {
		if (mPlugin.mFallbackZoneLookup) {
			ZoneLayer<T> zoneLayer = mLayers.get(layer);
			if (zoneLayer == null) {
				return null;
			}
			return zoneLayer.fallbackGetZone(loc);
		} else {
			return mZoneTree.getZone(loc, layer);
		}
	}

	public Zone<T> getZone(Location loc, String layer) {
		if (loc == null) {
			return null;
		}

		return mZoneTree.getZone(loc.toVector(), layer);
	}

	public boolean hasProperty(Vector loc, String layerName, String propertyName) {
		Zone<T> zone = getZone(loc, layerName);
		if (zone == null) {
			return false;
		}
		return zone.hasProperty(propertyName);
	}

	public boolean hasProperty(Location loc, String layerName, String propertyName) {
		if (loc == null) {
			return false;
		}
		return hasProperty(loc.toVector(), layerName, propertyName);
	}

	// Passing a player is optimized to use the last known location
	public boolean hasProperty(Player player, String layerName, String propertyName) {
		if (mPlugin.mFallbackZoneLookup) {
			if (player == null) {
				return false;
			}

			Map<String, Zone<T>> zones = mLastPlayerZones.get(player);
			if (zones == null) {
				return false;
			}

			Zone<T> zone = zones.get(layerName);
			if (zone == null) {
				return false;
			}

			return zone.hasProperty(propertyName);
		} else {
			ZoneFragment<T> lastFragment = mLastPlayerZoneFragment.get(player);
			if (lastFragment == null) {
				return false;
			}

			return lastFragment.hasProperty(layerName, propertyName);
		}
	}

	/************************************************************************************
	 * End of methods for use with external plugins:
	 ************************************************************************************/

	/*
	 * If sender is non-null, it will be sent debugging information
	 *
	 * In the event we have enough zones this takes a while to load:
	 * Reloading after the first startup could use an async load, only pausing long enough to swap
	 * the generated tree. If that's not good enough, an unbalanced tree is even faster to generate
	 * without too much slowdown, and the ZoneLayer class could be modified to handle determining
	 * which zones have priority if startup time NEEDS to be near-instant in exchange for slower
	 * clock speeds while the tree is loading. We have options.
	 */
	public void reload(Plugin plugin, CommandSender sender) {
		for (ZoneLayer<T> layer : mLayers.values()) {
			// Cause zones to stop tracking their fragments; speeds up garbage collection.
			layer.invalidate();
		}
		mLayers.clear();
		ZoneLayer.clearDynmapLayers();

		// Refresh plugin layers
		for (ZoneLayer<T> layer : mPluginLayers.values()) {
			layer.reloadFragments(sender);
		}

		mLayers.putAll(mPluginLayers);
		QuestUtils.loadScriptedQuests(plugin, "zone_layers", sender, (object) -> {
			// Load this file into a ZoneLayer object
			ZoneLayer<T> layer = new ZoneLayer<T>(sender, object);
			String layerName = layer.getName();

			if (mLayers.containsKey(layerName)) {
				throw new Exception("'" + layerName + "' already exists!");
			}

			mLayers.put(layerName, layer);

			return layerName + ":" + Integer.toString(layer.getZones().size());
		});

		// Merge zone fragments within layers to prevent overlaps
		mergeLayers();

		// Create list of zones
		List<Zone<T>> zones = new ArrayList<Zone<T>>();
		for (ZoneLayer<T> layer : mLayers.values()) {
			zones.addAll(layer.getZones());
		}

		// Defragment to reduce fragment count (approx 2-3x on average). This takes a long time.
		for (Zone<T> zone : zones) {
			zone.defragment();
		}

		// Create list of all zone fragments.
		List<ZoneFragment<T>> zoneFragments = new ArrayList<ZoneFragment<T>>();
		for (Zone<T> zone : zones) {
			zoneFragments.addAll(zone.getZoneFragments());
		}

		// Create the new tree. This could take a long time with enough fragments.
		BaseZoneTree<T> newTree;
		try {
			newTree = BaseZoneTree.createZoneTree(zoneFragments);
		} catch (Exception e) {
			MessagingUtils.sendStackTrace(sender, e);
			return;
		}
		if (sender != null) {
			sender.sendMessage(ChatColor.GOLD + "Zone tree stats - fragments: "
			                + Integer.toString(newTree.fragmentCount())
			                + ", max depth: "
			                + Integer.toString(newTree.maxDepth())
			                + ", ave depth: "
			                + String.format("%.2f", newTree.averageDepth()));
		}

		// Make sure only one player tracker runs at a time.
		if (mPlayerTracker != null && !mPlayerTracker.isCancelled()) {
			mPlayerTracker.cancel();
		}

		// Swap the tree out; this is really fast!
		BaseZoneTree<T> oldTree = mZoneTree;
		mZoneTree = newTree;
		if (oldTree != null) {
			// Force all fragments to consider all locations as outside themselves
			oldTree.invalidate();
		}

		// Start a new task to track players changing zones
		mPlayerTracker = new BukkitRunnable() {
			@Override
			public void run() {
				for (Player player : Bukkit.getOnlinePlayers()) {
					Location playerLocation = player.getLocation();
					Vector playerVector = playerLocation.toVector();

					if (mPlugin.mFallbackZoneLookup) {
						// getZones() will use fallback zone lookup in this case
						Map<String, Zone<T>> currentZones = getZones(playerVector);
						// Need to check all layer names, not just the ones the player is in
						for (String layerName : mLayers.keySet()) {
							Zone<T> currentZone = currentZones.get(layerName);
							// Handles comparing to previous zone if needed
							applyZoneChange(player, layerName, currentZone);
						}
					}

					ZoneFragment<T> lastZoneFragment = mLastPlayerZoneFragment.get(player);
					if (lastZoneFragment != null && lastZoneFragment.within(playerVector)) {
						// Player has not left their previous zone fragment; no zone change.
						// Note that reloading invalidates previous zones, causing within() to
						// return false regardless of if the player would have been within the fragment.
						continue;
					}

					ZoneFragment<T> currentZoneFragment = getZoneFragment(playerVector);
					if (lastZoneFragment == null && currentZoneFragment == null) {
						// Player was not in a previous zone fragment, and isn't in one now; no zone change.
						continue;
					}

					applyFragmentChange(player, currentZoneFragment);
				}
			}
		};

		mPlayerTracker.runTaskTimer(plugin, 0, 5);
	}

	// For a given location, return the zones that contain it.
	private Map<String, Zone<T>> getZonesInternal(Vector loc, boolean fallbackZoneLookup) {
		if (fallbackZoneLookup) {
			Map<String, Zone<T>> result = new HashMap<String, Zone<T>>();
			for (Map.Entry<String, ZoneLayer<T>> entry : mLayers.entrySet()) {
				String layerName = entry.getKey();
				ZoneLayer<T> zoneLayer = entry.getValue();

				Zone<T> zone = zoneLayer.fallbackGetZone(loc);
				if (zone != null) {
					result.put(layerName, zone);
				}
			}
			return result;
		} else {
			return mZoneTree.getZones(loc);
		}
	}

	public void unregisterPlayer(Player player) {
		applyFragmentChange(player, null);
		if (mPlugin.mFallbackZoneLookup && mLastPlayerZones.get(player) != null) {
			// Copy key set, as we are modifying the map during iteration
			Set<String> layerNames = new LinkedHashSet<String>(mLastPlayerZones.get(player).keySet());
			for (String layerName : layerNames) {
				applyZoneChange(player, layerName, null);
			}
		}
	}

	private void applyFragmentChange(Player player, ZoneFragment<T> currentZoneFragment) {
		ZoneFragment<T> lastZoneFragment = mLastPlayerZoneFragment.get(player);

		Map<String, Zone<T>> lastZones = new HashMap<String, Zone<T>>();
		if (lastZoneFragment != null) {
			lastZones = lastZoneFragment.getParents();
		}

		Map<String, Zone<T>> currentZones = new HashMap<String, Zone<T>>();
		if (currentZoneFragment != null) {
			currentZones = currentZoneFragment.getParents();
		}

		// We've already confirmed the player changed zone fragments; null is valid.
		mLastPlayerZoneFragment.put(player, currentZoneFragment);

		if ((currentZones == null && lastZones == null) ||
		    (currentZones != null && lastZones != null && currentZones.equals(lastZones))) {
			// If the zones are identical between both fragments, nothing more to do.
			// Zones and ZoneFragments are not cloned after the manager is instantiated; == is valid.
			return;
		}

		if (!mPlugin.mFallbackZoneLookup) {
			// Zones changed, send an event for each layer.
			Set<String> mentionedLayerNames = new LinkedHashSet<String>(lastZones.keySet());
			mentionedLayerNames.addAll(currentZones.keySet());
			for (String layerName : mentionedLayerNames) {
				// Null zones are valid - indicates no zone.
				Zone<T> currentZone = currentZones.get(layerName);
				applyZoneChange(player, layerName, currentZone);
			}
		}
	}

	private void applyZoneChange(Player player, String layerName, Zone<T> currentZone) {
		// Null zones are valid - indicates no zone.
		Map<String, Zone<T>> lastZones = mLastPlayerZones.get(player);
		if (lastZones == null) {
			lastZones = new HashMap<String, Zone<T>>();
			mLastPlayerZones.put(player, lastZones);
		}

		Zone<T> lastZone = lastZones.get(layerName);
		if (lastZone == currentZone) {
			// Nothing to do!
			return;
		}

		ZoneChangeEvent zoneEvent = new ZoneChangeEvent(player, layerName, lastZone, currentZone);
		Bukkit.getPluginManager().callEvent(zoneEvent);

		Set<String> lastProperties;
		if (lastZone == null) {
			lastProperties = new LinkedHashSet<String>();
		} else {
			lastProperties = lastZone.getProperties();
		}

		Set<String> currentProperties;
		if (currentZone == null) {
			currentProperties = new LinkedHashSet<String>();
		} else {
			currentProperties = currentZone.getProperties();
		}

		Set<String> removedProperties = new LinkedHashSet<String>(lastProperties);
		removedProperties.removeAll(currentProperties);
		for (String property : removedProperties) {
			ZonePropertyChangeEvent event;
			event = new ZonePropertyChangeEvent(player, layerName, "!" + property);
			Bukkit.getPluginManager().callEvent(event);
		}

		Set<String> addedProperties = new LinkedHashSet<String>(currentProperties);
		addedProperties.removeAll(lastProperties);
		for (String property : addedProperties) {
			ZonePropertyChangeEvent event;
			event = new ZonePropertyChangeEvent(player, layerName, property);
			Bukkit.getPluginManager().callEvent(event);
		}

		if (currentZone == null) {
			lastZones.remove(layerName);
			if (lastZones.size() == 0) {
				mLastPlayerZones.remove(player);
			}
		} else {
			lastZones.put(layerName, currentZone);
		}
	}

	private void mergeLayers() {
		List<ZoneLayer<T>> layers = new ArrayList<ZoneLayer<T>>(mLayers.values());

		int numLayers = layers.size();
		for (int i = 0; i < numLayers; i++) {
			ZoneLayer<T> outer = layers.get(i);
			for (int j = i + 1; j < numLayers; j++) {
				ZoneLayer<T> inner = layers.get(j);
				mergeLayers(outer, inner);
			}
		}
	}

	private void mergeLayers(ZoneLayer<T> outerLayer, ZoneLayer<T> innerLayer) {
		for (Zone<T> outerZone : outerLayer.getZones()) {
			for (Zone<T> innerZone : innerLayer.getZones()) {
				BaseZone overlap = outerZone.overlappingZone(innerZone);
				if (overlap == null) {
					continue;
				}
				outerZone.splitByOverlap(overlap, innerZone, true);
				innerZone.splitByOverlap(overlap, outerZone);
			}
		}
	}

	/*
	 * sender recieves the debug info, player is the target and sees nothing
	 */
	public void sendDebug(CommandSender sender, Player player) {
		Vector playerLocation = player.getLocation().toVector();

		sender.sendMessage("Cached player info according to zone fragment tree:");

		ZoneFragment<T> cachedFragment = mLastPlayerZoneFragment.get(player);
		if (cachedFragment == null) {
			sender.sendMessage("Target is not in any zone.");
		} else {
			sender.sendMessage(cachedFragment.toString());

			Map<String, Zone<T>> fragmentParents = cachedFragment.getParents();
			if (fragmentParents.size() == 0) {
				sender.sendMessage("Fragment has no parent zones! Where did it come from?");
			}
			for (Zone<T> zone : fragmentParents.values()) {
				sender.sendMessage(zone.toString());
			}
		}

		if (mPlugin.mFallbackZoneLookup) {
			sender.sendMessage("Cached player info according to slow/reliable method:");

			Map<String, Zone<T>> cachedZones = mLastPlayerZones.get(player);
			if (cachedZones == null) {
				sender.sendMessage("Target is not in any zone.");
			} else {
				if (cachedZones.size() == 0) {
					sender.sendMessage("Target is not in any zone, but an empty map is left over.");
				}
				for (Zone<T> cachedZone : cachedZones.values()) {
					sender.sendMessage(cachedZone.toString());
				}
			}
		}

		sender.sendMessage("Uncached location info:");
		sendDebug(sender, playerLocation);
	}

	public void sendDebug(CommandSender sender, Vector loc) {
		Map<String, Zone<T>> fallbackZones = getZonesInternal(loc, true);
		Map<String, Zone<T>> fastZones = getZonesInternal(loc, false);
		if (fallbackZones == null && fastZones == null) {
			sender.sendMessage("Fast lookup matches slow/reliable lookup (both null)");
		} else if (fallbackZones == null || fastZones == null || !fallbackZones.equals(fastZones)) {
			sender.sendMessage("Fast lookup DOES NOT match slow/reliable lookup");

			sender.sendMessage("Slow/reliable lookup version:");
			if (fallbackZones == null) {
				sender.sendMessage("Target is not in any zone.");
			} else {
				if (fallbackZones.size() == 0) {
					sender.sendMessage("Target is not in any zone, but an empty map is left over.");
				}
				for (Zone<T> fallbackZone : fallbackZones.values()) {
					sender.sendMessage(fallbackZone.toString());
				}
			}
		} else {
			sender.sendMessage("Fast lookup matches slow/reliable lookup (both exist and are equal)");
		}

		sender.sendMessage("Fast lookup version:");
		ZoneFragment<T> fragment = getZoneFragment(loc);
		if (fragment == null) {
			sender.sendMessage("Target is not in any zone.");
			return;
		}

		if (!fragment.within(loc)) {
			sender.sendMessage("Target is not in the zone fragment they were reported in.");
		}
		sender.sendMessage(fragment.toString());

		Map<String, Zone<T>> fragmentParents = fragment.getParents();
		if (fragmentParents.size() == 0) {
			sender.sendMessage("Fragment has no parent zones! Where did it come from?");
		}
		for (Zone<T> zone : fragmentParents.values()) {
			sender.sendMessage(zone.toString());
		}
	}
}
