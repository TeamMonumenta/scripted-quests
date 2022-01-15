package com.playmonumenta.scriptedquests.zones;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.ArgUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ZoneManager {
	private Plugin mPlugin;
	static @MonotonicNonNull BukkitRunnable mPlayerTracker = null;
	static @Nullable BukkitRunnable mAsyncReloadHandler = null;

	private Map<String, ZoneLayer> mLayers = new HashMap<String, ZoneLayer>();
	private Map<String, ZoneLayer> mPluginLayers = new HashMap<String, ZoneLayer>();
	private @MonotonicNonNull ZoneTreeBase mZoneTree = null;

	private Map<UUID, ZoneFragment> mLastPlayerZoneFragment = new HashMap<UUID, ZoneFragment>();
	private Map<UUID, Map<String, Zone>> mLastPlayerZones = new HashMap<UUID, Map<String, Zone>>();

	private Set<CommandSender> mReloadRequesters = new HashSet<CommandSender>();
	private Set<CommandSender> mQueuedReloadRequesters = new HashSet<CommandSender>();

	public ZoneManager(Plugin plugin) {
		mPlugin = plugin;
		mQueuedReloadRequesters.add(Bukkit.getConsoleSender());
		doReload(plugin);
	}

	/************************************************************************************
	 * Start of methods for use with external plugins:
	 ************************************************************************************/

	/*
	 * Register a ZoneLayer from an external plugin.
	 *
	 * Returns true on success, false on failure.
	 */
	public boolean registerPluginZoneLayer(ZoneLayer layer) {
		if (layer == null) {
			return false;
		}

		String layerName = layer.getName();

		if (mPluginLayers.containsKey(layerName)) {
			return false;
		}

		mPluginLayers.put(layerName, layer);
		reload(mPlugin, Bukkit.getConsoleSender());
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
		reload(mPlugin, Bukkit.getConsoleSender());
		return true;
	}

	/*
	 * Replace a ZoneLayer from an external plugin.
	 *
	 * Be sure to call invalidate() on the old layer before discarding to ease garbage collection.
	 *
	 * Returns true on success, false on failure.
	 */
	public boolean replacePluginZoneLayer(ZoneLayer layer) {
		if (layer == null) {
			return false;
		}

		String layerName = layer.getName();

		mPluginLayers.put(layerName, layer);
		reload(mPlugin, Bukkit.getConsoleSender());
		return true;
	}

	/*
	 * Returns all ZoneFragments that overlap a bounding box.
	 */
	public Set<ZoneFragment> getZoneFragments(BoundingBox bb) {
		return mZoneTree.getZoneFragments(bb);
	}

	/*
	 * For a given location, return the fragment that contains it.
	 * Returns null if no fragment overlaps it.
	 *
	 * If fallback zone lookup is enabled, this should be avoided.
	 */
	public @Nullable ZoneFragment getZoneFragment(Vector loc) {
		return mZoneTree.getZoneFragment(loc);
	}

	public @Nullable ZoneFragment getZoneFragment(Location loc) {
		if (loc == null) {
			return null;
		}

		return mZoneTree.getZoneFragment(loc.toVector());
	}

	// For a given location, return the zones that contain it.
	public Map<String, Zone> getZones(Vector loc) {
		return getZonesInternal(loc, mPlugin.mFallbackZoneLookup);
	}

	public Map<String, Zone> getZones(Location loc) {
		if (loc == null) {
			return new HashMap<String, Zone>();
		}

		return mZoneTree.getZones(loc.toVector());
	}

	/*
	 * Returns all zones that overlap a bounding box, optionally including eclipsed zones.
	 */
	public Set<Zone> getZones(BoundingBox bb, boolean includeEclipsed) {
		return mZoneTree.getZones(bb, includeEclipsed);
	}

	/*
	 * For a given layer and location, return the zone that
	 * contains it. Returns null if no zone overlaps it.
	 */
	public @Nullable Zone getZone(Vector loc, String layer) {
		if (mPlugin.mFallbackZoneLookup) {
			@Nullable ZoneLayer zoneLayer = mLayers.get(layer);
			if (zoneLayer == null) {
				return null;
			}
			return zoneLayer.fallbackGetZone(loc);
		} else {
			return mZoneTree.getZone(loc, layer);
		}
	}

	public @Nullable Zone getZone(Location loc, String layer) {
		if (loc == null) {
			return null;
		}

		return mZoneTree.getZone(loc.toVector(), layer);
	}

	public boolean hasProperty(Vector loc, String layerName, String propertyName) {
		@Nullable Zone zone = getZone(loc, layerName);
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
		if (player == null) {
			return false;
		}
		UUID playerUuid = player.getUniqueId();

		if (mPlugin.mFallbackZoneLookup) {
			@Nullable Map<String, Zone> zones = mLastPlayerZones.get(playerUuid);
			if (zones == null) {
				return false;
			}

			@Nullable Zone zone = zones.get(layerName);
			if (zone == null) {
				return false;
			}

			return zone.hasProperty(propertyName);
		} else {
			@Nullable ZoneFragment lastFragment = mLastPlayerZoneFragment.get(playerUuid);
			if (lastFragment == null) {
				return false;
			}

			return lastFragment.hasProperty(layerName, propertyName);
		}
	}

	public Set<String> getLayerNames() {
		return new HashSet<String>(mLayers.keySet());
	}

	public String[] getLayerNameSuggestions() {
		return ArgUtils.quoteIfNeeded(new TreeSet<String>(getLayerNames()));
	}

	public Set<String> getLoadedProperties(String layerName) {
		@Nullable ZoneLayer layer = mLayers.get(layerName);
		if (layer == null) {
			return new HashSet<String>();
		}
		return layer.getLoadedProperties();
	}

	public String[] getLoadedPropertySuggestions(String layerName) {
		Set<String> properties = getLoadedProperties(layerName);
		Set<String> suggestions = new TreeSet<String>(properties);
		for (String property : properties) {
			suggestions.add("!" + property);
		}
		return ArgUtils.quoteIfNeeded(suggestions);
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
		if (sender == null) {
			sender = Bukkit.getConsoleSender();
		}
		mQueuedReloadRequesters.add(sender);
		sender.sendMessage(ChatColor.GOLD + "Zone reload started in the background, you will be notified of progress.");
		if (mAsyncReloadHandler == null) {
			// Start a new async task to handle reloads
			mAsyncReloadHandler = new BukkitRunnable() {
				@Override
				public void run() {
					try {
						handleReloads(plugin);
					} catch (Exception e) {
						MessagingUtils.sendStackTrace(mReloadRequesters, e);

						for (@Nullable CommandSender sender : mReloadRequesters) {
							if (sender != null) {
								sender.sendMessage(ChatColor.RED + "Zones failed to reload.");
							}
						}

						return;
					}
					mAsyncReloadHandler = null;
				}
			};

			mAsyncReloadHandler.runTaskAsynchronously(plugin);
		}
	}

	private void handleReloads(Plugin plugin) {
		do {
			doReload(plugin);
		} while (!mQueuedReloadRequesters.isEmpty());
	}

	private void doReload(Plugin plugin) {
		mReloadRequesters = mQueuedReloadRequesters;
		mQueuedReloadRequesters = new HashSet<CommandSender>();
		mReloadRequesters.add(Bukkit.getConsoleSender());

		for (ZoneLayer layer : mLayers.values()) {
			// Cause zones to stop tracking their fragments; speeds up garbage collection.
			layer.invalidate();
		}
		mLayers.clear();
		ZoneLayer.clearDynmapLayers();

		// Refresh plugin layers
		for (ZoneLayer layer : mPluginLayers.values()) {
			layer.reloadFragments(mReloadRequesters);
		}

		mLayers.putAll(mPluginLayers);
		QuestUtils.loadScriptedQuests(plugin, "zone_layers", mReloadRequesters, (object) -> {
			// Load this file into a ZoneLayer object
			ZoneLayer layer = new ZoneLayer(mReloadRequesters, object);
			String layerName = layer.getName();

			if (mLayers.containsKey(layerName)) {
				throw new Exception("'" + layerName + "' already exists!");
			}

			mLayers.put(layerName, layer);

			return layerName + ":" + Integer.toString(layer.getZones().size());
		});

		for (@Nullable CommandSender sender : mReloadRequesters) {
			if (sender != null) {
				sender.sendMessage(ChatColor.GOLD + "Zone parsing successful, optimizing before enabling...");
			}
		}

		// Merge zone fragments within layers to prevent overlaps
		mergeLayers();

		// Create list of zones
		List<Zone> zones = new ArrayList<Zone>();
		for (ZoneLayer layer : mLayers.values()) {
			zones.addAll(layer.getZones());
		}

		// Defragment to reduce fragment count (approx 2-3x on average). This takes a long time.
		for (Zone zone : zones) {
			zone.defragment();
		}

		// Create list of all zone fragments.
		List<ZoneFragment> zoneFragments = new ArrayList<ZoneFragment>();
		for (Zone zone : zones) {
			zoneFragments.addAll(zone.getZoneFragments());
		}

		// Create the new tree. This could take a long time with enough fragments.
		ZoneTreeBase newTree;
		try {
			newTree = ZoneTreeBase.createZoneTree(zoneFragments);
		} catch (Exception e) {
			MessagingUtils.sendStackTrace(mReloadRequesters, e);
			return;
		}
		String message = ChatColor.GOLD + "Zone tree dev stats - fragments: "
		               + Integer.toString(newTree.fragmentCount())
		               + ", max depth: "
		               + Integer.toString(newTree.maxDepth())
		               + ", ave depth: "
		               + String.format("%.2f", newTree.averageDepth());
		for (@Nullable CommandSender sender : mReloadRequesters) {
			if (sender != null) {
				sender.sendMessage(message);
			}
		}

		// Make sure only one player tracker runs at a time.
		if (mPlayerTracker != null && !mPlayerTracker.isCancelled()) {
			mPlayerTracker.cancel();
		}

		// Swap the tree out; this is really fast!
		@Nullable ZoneTreeBase oldTree = mZoneTree;
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
					UUID playerUuid = player.getUniqueId();
					Location playerLocation = player.getLocation();
					Vector playerVector = playerLocation.toVector();

					if (mPlugin.mFallbackZoneLookup) {
						// getZones() will use fallback zone lookup in this case
						Map<String, Zone> currentZones = getZones(playerVector);
						// Need to check all layer names, not just the ones the player is in
						for (String layerName : mLayers.keySet()) {
							Zone currentZone = currentZones.get(layerName);
							// Handles comparing to previous zone if needed
							applyZoneChange(player, layerName, currentZone);
						}
					}

					@Nullable ZoneFragment lastZoneFragment = mLastPlayerZoneFragment.get(playerUuid);
					if (lastZoneFragment != null && lastZoneFragment.within(playerVector)) {
						// Player has not left their previous zone fragment; no zone change.
						// Note that reloading invalidates previous zones, causing within() to
						// return false regardless of if the player would have been within the fragment.
						continue;
					}

					@Nullable ZoneFragment currentZoneFragment = getZoneFragment(playerVector);
					if (lastZoneFragment == null && currentZoneFragment == null) {
						// Player was not in a previous zone fragment, and isn't in one now; no zone change.
						continue;
					}

					applyFragmentChange(player, currentZoneFragment);
				}
			}
		};

		mPlayerTracker.runTaskTimer(plugin, 0, 5);

		for (@Nullable CommandSender sender : mReloadRequesters) {
			if (sender != null) {
				sender.sendMessage(ChatColor.GOLD + "Zones reloaded successfully.");
			}
		}
	}

	// For a given location, return the zones that contain it.
	private Map<String, Zone> getZonesInternal(Vector loc, boolean fallbackZoneLookup) {
		if (fallbackZoneLookup) {
			Map<String, Zone> result = new HashMap<String, Zone>();
			for (Map.Entry<String, ZoneLayer> entry : mLayers.entrySet()) {
				String layerName = entry.getKey();
				ZoneLayer zoneLayer = entry.getValue();

				@Nullable Zone zone = zoneLayer.fallbackGetZone(loc);
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
		UUID playerUuid = player.getUniqueId();
		applyFragmentChange(player, null);
		if (mPlugin.mFallbackZoneLookup && mLastPlayerZones.get(playerUuid) != null) {
			// Copy key set, as we are modifying the map during iteration
			Set<String> layerNames = new LinkedHashSet<String>(mLastPlayerZones.get(playerUuid).keySet());
			for (String layerName : layerNames) {
				applyZoneChange(player, layerName, null);
			}
		}
		mLastPlayerZoneFragment.remove(playerUuid);
		mLastPlayerZones.remove(playerUuid);
	}

	private void applyFragmentChange(Player player, @Nullable ZoneFragment currentZoneFragment) {
		UUID playerUuid = player.getUniqueId();
		@Nullable ZoneFragment lastZoneFragment = mLastPlayerZoneFragment.get(playerUuid);

		Map<String, Zone> lastZones = new HashMap<String, Zone>();
		if (lastZoneFragment != null) {
			lastZones = lastZoneFragment.getParents();
		}

		Map<String, Zone> currentZones = new HashMap<String, Zone>();
		if (currentZoneFragment != null) {
			currentZones = currentZoneFragment.getParents();
		}

		// We've already confirmed the player changed zone fragments; null is valid.
		mLastPlayerZoneFragment.put(playerUuid, currentZoneFragment);

		if (currentZones.equals(lastZones)) {
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
				@Nullable Zone currentZone = currentZones.get(layerName);
				applyZoneChange(player, layerName, currentZone);
			}
		}
	}

	private void applyZoneChange(Player player, String layerName, @Nullable Zone currentZone) {
		UUID playerUuid = player.getUniqueId();
		// Null zones are valid - indicates no zone.
		@Nullable Map<String, Zone> lastZones = mLastPlayerZones.get(playerUuid);
		if (lastZones == null) {
			lastZones = new HashMap<String, Zone>();
			mLastPlayerZones.put(playerUuid, lastZones);
		}

		@Nullable Zone lastZone = lastZones.get(layerName);
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
				mLastPlayerZones.remove(playerUuid);
			}
		} else {
			lastZones.put(layerName, currentZone);
		}
	}

	private void mergeLayers() {
		List<ZoneLayer> layers = new ArrayList<ZoneLayer>(mLayers.values());

		int numLayers = layers.size();
		for (int i = 0; i < numLayers; i++) {
			ZoneLayer outer = layers.get(i);
			for (int j = i + 1; j < numLayers; j++) {
				ZoneLayer inner = layers.get(j);
				mergeLayers(outer, inner);
			}
		}
	}

	private void mergeLayers(ZoneLayer outerLayer, ZoneLayer innerLayer) {
		for (Zone outerZone : outerLayer.getZones()) {
			for (Zone innerZone : innerLayer.getZones()) {
				@Nullable ZoneBase overlap = outerZone.overlappingZone(innerZone);
				if (overlap == null) {
					continue;
				}
				outerZone.splitByOverlap(overlap, innerZone, true);
				outerZone.defragment();
				innerZone.splitByOverlap(overlap, outerZone);
				innerZone.defragment();
			}
		}
	}

	/*
	 * sender recieves the debug info, player is the target and sees nothing
	 */
	public void sendDebug(CommandSender sender, Player player) {
		if (sender == null) {
			return;
		}

		UUID playerUuid = player.getUniqueId();
		Vector playerLocation = player.getLocation().toVector();

		sender.sendMessage("Cached player info according to zone fragment tree:");

		@Nullable ZoneFragment cachedFragment = mLastPlayerZoneFragment.get(playerUuid);
		if (cachedFragment == null) {
			sender.sendMessage("Target is not in any zone.");
		} else {
			sender.sendMessage(cachedFragment.toString());

			Map<String, Zone> fragmentParents = cachedFragment.getParents();
			if (fragmentParents.size() == 0) {
				sender.sendMessage("Fragment has no parent zones! Where did it come from?");
			}
			sender.sendMessage("Fragment parents:");
			for (Zone zone : fragmentParents.values()) {
				sender.sendMessage(zone.toString());
			}
			sender.sendMessage("Fragment parents and eclipsed:");
			for (List<Zone> zones : cachedFragment.getParentsAndEclipsed().values()) {
				for (Zone zone : zones) {
					sender.sendMessage(zone.toString());
				}
			}
		}

		if (mPlugin.mFallbackZoneLookup) {
			sender.sendMessage("Cached player info according to slow/reliable method:");

			@Nullable Map<String, Zone> cachedZones = mLastPlayerZones.get(playerUuid);
			if (cachedZones == null) {
				sender.sendMessage("Target is not in any zone.");
			} else {
				if (cachedZones.size() == 0) {
					sender.sendMessage("Target is not in any zone, but an empty map is left over.");
				}
				for (Zone cachedZone : cachedZones.values()) {
					sender.sendMessage(cachedZone.toString());
				}
			}
		}

		sender.sendMessage("Uncached location info:");
		sendDebug(sender, playerLocation);
	}

	public void sendDebug(CommandSender sender, Vector loc) {
		if (sender == null) {
			return;
		}

		@Nullable Map<String, Zone> fallbackZones = getZonesInternal(loc, true);
		@Nullable Map<String, Zone> fastZones = getZonesInternal(loc, false);
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
				for (Zone fallbackZone : fallbackZones.values()) {
					sender.sendMessage(fallbackZone.toString());
				}
			}
		} else {
			sender.sendMessage("Fast lookup matches slow/reliable lookup (both exist and are equal)");
		}

		sender.sendMessage("Fast lookup version:");
		@Nullable ZoneFragment fragment = getZoneFragment(loc);
		if (fragment == null) {
			sender.sendMessage("Target is not in any zone.");
			return;
		}

		if (!fragment.within(loc)) {
			sender.sendMessage("Target is not in the zone fragment they were reported in.");
		}
		sender.sendMessage(fragment.toString());

		Map<String, Zone> fragmentParents = fragment.getParents();
		if (fragmentParents.size() == 0) {
			sender.sendMessage("Fragment has no parent zones! Where did it come from?");
		}
		sender.sendMessage("Fragment parents:");
		for (Zone zone : fragmentParents.values()) {
			sender.sendMessage(zone.toString());
		}
		sender.sendMessage("Fragment parents and eclipsed:");
		for (List<Zone> zones : fragment.getParentsAndEclipsed().values()) {
			for (Zone zone : zones) {
				sender.sendMessage(zone.toString());
			}
		}
	}
}
