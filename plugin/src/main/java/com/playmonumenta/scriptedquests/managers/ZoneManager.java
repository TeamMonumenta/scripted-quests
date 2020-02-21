package com.playmonumenta.scriptedquests.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
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

	private HashMap<String, ZoneLayer<T>> mLayers = new HashMap<String, ZoneLayer<T>>();
	private HashMap<String, ZoneLayer<T>> mPluginLayers = new HashMap<String, ZoneLayer<T>>();
	private BaseZoneTree<T> mZoneTree = null;
	private Map<Player, ZoneFragment<T>> mLastPlayerZoneFragment = new HashMap<Player, ZoneFragment<T>>();

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

		mLayers.put(layerName, layer);
		reload(mPlugin, null);
		return true;
	}

	/*
	 * Unregister a ZoneLayer from an external plugin.
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
	 * For a given location, return the fragment that contains it.
	 * Returns null if no fragment overlaps it.
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
	public HashMap<String, Zone<T>> getZones(Vector loc) {
		return mZoneTree.getZones(loc);
	}

	public HashMap<String, Zone<T>> getZones(Location loc) {
		if (loc == null) {
			return null;
		}

		return mZoneTree.getZones(loc.toVector());
	}

	/*
	 * For a given layer and location, return the zone that
	 * contains it. Returns null if no zone overlaps it.
	 */
	public Zone<T> getZone(Vector loc, String layer) {
		return mZoneTree.getZone(loc, layer);
	}

	public Zone<T> getZone(Location loc, String layer) {
		if (loc == null) {
			return null;
		}

		return mZoneTree.getZone(loc.toVector(), layer);
	}

	public boolean hasProperty(Vector loc, String layerName, String propertyName) {
		return mZoneTree != null && mZoneTree.hasProperty(loc, layerName, propertyName);
	}

	public boolean hasProperty(Location loc, String layerName, String propertyName) {
		return mZoneTree != null && mZoneTree.hasProperty(loc.toVector(), layerName, propertyName);
	}

	// Passing a player is optimized to use the last known location
	public boolean hasProperty(Player player, String layerName, String propertyName) {
		ZoneFragment<T> lastFragment = mLastPlayerZoneFragment.get(player);
		if (lastFragment == null) {
			return false;
		}

		return lastFragment.hasProperty(layerName, propertyName);
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
		ArrayList<Zone<T>> zones = new ArrayList<Zone<T>>();
		for (ZoneLayer<T> layer : mLayers.values()) {
			zones.addAll(layer.getZones());
		}

		// Defragment to reduce fragment count (approx 2-3x on average). This takes a long time.
		for (Zone<T> zone : zones) {
			zone.defragment();
		}

		// Create list of all zone fragments.
		ArrayList<ZoneFragment<T>> zoneFragments = new ArrayList<ZoneFragment<T>>();
		for (Zone<T> zone : zones) {
			zoneFragments.addAll(zone.getZoneFragments());
		}

		// Create the new tree. This could take a long time with enough fragments.
		BaseZoneTree<T> newTree;
		try {
			newTree = BaseZoneTree.CreateZoneTree(zoneFragments);
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

					applyZoneChange(player, currentZoneFragment);
				}
			}
		};

		mPlayerTracker.runTaskTimer(plugin, 0, 5);
	}

	public void unregisterPlayer(Player player) {
		applyZoneChange(player, null);
	}

	private void applyZoneChange(Player player, ZoneFragment<T> currentZoneFragment) {
		ZoneFragment<T> lastZoneFragment = mLastPlayerZoneFragment.get(player);

		HashMap<String, Zone<T>> lastZones = new HashMap<String, Zone<T>>();
		if (lastZoneFragment != null) {
			lastZones = lastZoneFragment.getParents();
		}

		HashMap<String, Zone<T>> currentZones = new HashMap<String, Zone<T>>();
		if (currentZoneFragment != null) {
			currentZones = currentZoneFragment.getParents();
		}

		// We've already confirmed the player changed zone fragments; null is valid.
		mLastPlayerZoneFragment.put(player, currentZoneFragment);

		if (currentZones == lastZones) {
			// If the zones are identical between both fragments, nothing more to do.
			// Zones and ZoneFragments are not cloned after the manager is instantiated; == is valid.
			return;
		}

		// Zones changed, send an event for each layer.
		Set<String> mentionedLayerNames = new LinkedHashSet<String>(lastZones.keySet());
		mentionedLayerNames.addAll(currentZones.keySet());
		for (String layerName : mentionedLayerNames) {
			// Null zones are valid - indicates no zone.
			Zone<T> lastZone = lastZones.get(layerName);
			Zone<T> currentZone = currentZones.get(layerName);

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
		}
	}

	private void mergeLayers() {
		ArrayList<ZoneLayer<T>> layers = new ArrayList<ZoneLayer<T>>(mLayers.values());

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
}
