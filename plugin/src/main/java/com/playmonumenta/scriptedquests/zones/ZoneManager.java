package com.playmonumenta.scriptedquests.zones;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.ArgUtils;
import com.playmonumenta.scriptedquests.utils.MMLog;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class ZoneManager {
	private static final String[] SUGGESTIONS_EXECUTE_FALLBACK = {"\"Suggestions unavailable through /execute\""};

	private final Plugin mPlugin;
	private static @MonotonicNonNull ZoneManager INSTANCE = null;
	static @MonotonicNonNull BukkitRunnable mPlayerTracker = null;
	static @Nullable BukkitRunnable mAsyncReloadHandler = null;

	private final Map<String, ZoneNamespace> mNamespaces = new HashMap<>();
	private final Map<String, ZoneNamespace> mPluginNamespaces = new HashMap<>();
	private @MonotonicNonNull ZoneTreeBase mZoneTree = null;

	private final Set<UUID> mTransferringPlayers = new HashSet<>();
	private final Map<UUID, ZoneFragment> mLastPlayerZoneFragment = new HashMap<>();
	private final Map<UUID, Map<String, Zone>> mLastPlayerZones = new HashMap<>();

	private Set<CommandSender> mReloadRequesters = new HashSet<>();
	private Set<CommandSender> mQueuedReloadRequesters = new HashSet<>();

	private ZoneManager(Plugin plugin) {
		mPlugin = plugin;
		mQueuedReloadRequesters.add(Bukkit.getConsoleSender());
	}

	public static ZoneManager createInstance(Plugin plugin) {
		INSTANCE = new ZoneManager(plugin);
		return INSTANCE;
	}

	public static ZoneManager getInstance() {
		if (INSTANCE == null) {
			throw new RuntimeException("Attempted to access ZoneManager before initialization");
		}
		return INSTANCE;
	}

	/************************************************************************************
	 * Start of methods for use with external plugins:
	 ************************************************************************************/

	/*
	 * Register a ZoneNamespace from an external plugin.
	 *
	 * Returns true on success, false on failure.
	 */
	public boolean registerPluginZoneNamespace(ZoneNamespace namespace) {
		if (namespace == null) {
			return false;
		}

		String namespaceName = namespace.getName();

		if (mPluginNamespaces.containsKey(namespaceName)) {
			return false;
		}

		mPluginNamespaces.put(namespaceName, namespace);
		reload(mPlugin, Bukkit.getConsoleSender());
		return true;
	}

	/*
	 * Unregister a ZoneNamespace from an external plugin.
	 *
	 * Be sure to call invalidate() on the old namespace before discarding to ease garbage collection.
	 *
	 * Returns true on success, false on failure.
	 */
	public boolean unregisterPluginZoneNamespace(String namespaceName) {
		if (!mPluginNamespaces.containsKey(namespaceName)) {
			return false;
		}

		mPluginNamespaces.remove(namespaceName);
		reload(mPlugin, Bukkit.getConsoleSender());
		return true;
	}

	/*
	 * Replace a ZoneNamespace from an external plugin.
	 *
	 * Be sure to call invalidate() on the old namespace before discarding to ease garbage collection.
	 *
	 * Returns true on success, false on failure.
	 */
	public boolean replacePluginZoneNamespace(ZoneNamespace namespace) {
		if (namespace == null) {
			return false;
		}

		String namespaceName = namespace.getName();

		mPluginNamespaces.put(namespaceName, namespace);
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
			return new HashMap<>();
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
	 * For a given namespace and location, return the zone that
	 * contains it. Returns null if no zone overlaps it.
	 */
	public @Nullable Zone getZone(Vector loc, String namespaceName) {
		if (mPlugin.mFallbackZoneLookup) {
			@Nullable ZoneNamespace zoneNamespace = mNamespaces.get(namespaceName);
			if (zoneNamespace == null) {
				return null;
			}
			return zoneNamespace.fallbackGetZone(loc);
		} else {
			return mZoneTree.getZone(loc, namespaceName);
		}
	}

	public @Nullable Zone getZone(Location loc, String namespaceName) {
		if (loc == null) {
			return null;
		}

		return mZoneTree.getZone(loc.toVector(), namespaceName);
	}

	public boolean hasProperty(Vector loc, String namespaceName, String propertyName) {
		@Nullable Zone zone = getZone(loc, namespaceName);
		if (zone == null) {
			return false;
		}
		return zone.hasProperty(propertyName);
	}

	public boolean hasProperty(Location loc, String namespaceName, String propertyName) {
		if (loc == null) {
			return false;
		}
		return hasProperty(loc.toVector(), namespaceName, propertyName);
	}

	// Passing a player is optimized to use the last known location
	public boolean hasProperty(Player player, String namespaceName, String propertyName) {
		if (player == null) {
			return false;
		}
		UUID playerUuid = player.getUniqueId();

		if (mPlugin.mFallbackZoneLookup) {
			@Nullable Map<String, Zone> zones = mLastPlayerZones.get(playerUuid);
			if (zones == null) {
				return false;
			}

			@Nullable Zone zone = zones.get(namespaceName);
			if (zone == null) {
				return false;
			}

			return zone.hasProperty(propertyName);
		} else {
			@Nullable ZoneFragment lastFragment = mLastPlayerZoneFragment.get(playerUuid);
			if (lastFragment == null) {
				return false;
			}

			return lastFragment.hasProperty(namespaceName, propertyName);
		}
	}

	public Set<String> getNamespaceNames() {
		return new HashSet<>(mNamespaces.keySet());
	}

	public String[] getNamespaceNameSuggestions() {
		return ArgUtils.quoteIfNeeded(new TreeSet<>(getNamespaceNames()));
	}

	public static ArgumentSuggestions getNamespaceArgumentSuggestions() {
		return ArgumentSuggestions.strings(info -> getInstance().getNamespaceNameSuggestions());
	}

	public Set<String> getLoadedProperties(String namespaceName) {
		@Nullable ZoneNamespace namespace = mNamespaces.get(namespaceName);
		if (namespace == null) {
			return new HashSet<>();
		}
		return namespace.getLoadedProperties();
	}

	public String[] getLoadedPropertySuggestions(String namespaceName) {
		Set<String> properties = getLoadedProperties(namespaceName);
		Set<String> suggestions = new TreeSet<>(properties);
		for (String property : properties) {
			suggestions.add("!" + property);
		}
		return ArgUtils.quoteIfNeeded(suggestions);
	}

	public static ArgumentSuggestions getLoadedPropertyArgumentSuggestions(String namespaceName) {
		return ArgumentSuggestions.strings(info -> getInstance().getLoadedPropertySuggestions(namespaceName));
	}

	public static ArgumentSuggestions getLoadedPropertyArgumentSuggestions(int namespaceNameArgIndex) {
		return ArgumentSuggestions.strings(info -> {
			Object[] args = info.previousArgs();
			if (args.length == 0) {
				return SUGGESTIONS_EXECUTE_FALLBACK;
			}

			int index = namespaceNameArgIndex;
			if (index < 0) {
				index += args.length;
			}

			if (index < 0 || index >= args.length) {
				return new String[]{"\"Invalid argument index for namespace name: " + index + "\""};
			}

			return getInstance().getLoadedPropertySuggestions((String) args[index]);
		});
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
	 * without too much slowdown, and the ZoneNamespace class could be modified to handle determining
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

	public void doReload(Plugin plugin) {
		MMLog.fine("[Zone Reload] Begin");
		mReloadRequesters = mQueuedReloadRequesters;
		mQueuedReloadRequesters = new HashSet<>();
		mReloadRequesters.add(Bukkit.getConsoleSender());

		long cpuNanos = System.nanoTime();
		for (ZoneNamespace namespace : mNamespaces.values()) {
			// Cause zones to stop tracking their fragments; speeds up garbage collection.
			namespace.invalidate();
		}
		mNamespaces.clear();
		ZoneNamespace.clearDynmapLayers();
		MMLog.fine("[Zone Reload] " + String.format("%13.9f", (System.nanoTime() - cpuNanos) / 1000000000.0) + "s Resetting old data");

		cpuNanos = System.nanoTime();
		plugin.mZonePropertyGroupManager.reload(plugin, mReloadRequesters);

		// Refresh plugin namespaces
		for (ZoneNamespace namespace : mPluginNamespaces.values()) {
			namespace.reloadFragments(mReloadRequesters);
		}

		mNamespaces.putAll(mPluginNamespaces);
		QuestUtils.loadScriptedQuests(plugin, "zone_namespaces", mReloadRequesters, (object) -> {
			// Load this file into a ZoneNamespace object
			ZoneNamespace namespace = new ZoneNamespace(mReloadRequesters, object);
			String namespaceName = namespace.getName();

			if (mNamespaces.containsKey(namespaceName)) {
				throw new Exception("'" + namespaceName + "' already exists!");
			}

			mNamespaces.put(namespaceName, namespace);

			return namespaceName + ":" + namespace.getZones().size();
		});
		MMLog.fine("[Zone Reload] " + String.format("%13.9f", (System.nanoTime() - cpuNanos) / 1000000000.0) + "s Loading new data");

		for (@Nullable CommandSender sender : mReloadRequesters) {
			if (sender != null) {
				sender.sendMessage(ChatColor.GOLD + "Zone parsing successful, optimizing before enabling...");
			}
		}

		// Merge zone fragments within namespaces to prevent overlaps
		cpuNanos = System.nanoTime();
		mergeNamespaces();
		MMLog.fine("[Zone Reload] " + String.format("%13.9f", (System.nanoTime() - cpuNanos) / 1000000000.0) + "s Merging namespace data");

		// Create list of zones
		List<Zone> zones = new ArrayList<>();
		for (ZoneNamespace namespace : mNamespaces.values()) {
			zones.addAll(namespace.getZones());
		}

		// Create list of all zone fragments.
		List<ZoneFragment> zoneFragments = new ArrayList<>();
		for (Zone zone : zones) {
			zoneFragments.addAll(zone.getZoneFragments());
		}

		// Create the new tree. This could take a long time with enough fragments.
		ZoneTreeBase newTree;
		try {
			cpuNanos = System.nanoTime();
			newTree = ZoneTreeBase.createZoneTree(zoneFragments);
			MMLog.fine("[Zone Reload] " + String.format("%13.9f", (System.nanoTime() - cpuNanos) / 1000000000.0) + "s Creating tree");
			if (mPlugin.mShowZonesDynmap) {
				newTree.refreshDynmapTree();
			}
		} catch (Exception e) {
			MessagingUtils.sendStackTrace(mReloadRequesters, e);
			return;
		}
		String message = ChatColor.GOLD + "Zone tree dev stats - fragments: "
		               + newTree.fragmentCount()
		               + ", max depth: "
		               + newTree.maxDepth()
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
						// Need to check all namespace names, not just the ones the player is in
						for (String namespaceName : mNamespaces.keySet()) {
							Zone currentZone = currentZones.get(namespaceName);
							// Handles comparing to previous zone if needed
							applyZoneChange(player, namespaceName, currentZone);
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

		mPlayerTracker.runTaskTimer(plugin, 0, 1);

		for (@Nullable CommandSender sender : mReloadRequesters) {
			if (sender != null) {
				sender.sendMessage(ChatColor.GOLD + "Zones reloaded successfully.");
			}
		}
	}

	public void treeLoad(CommandSender sender) {
		Set<CommandSender> senders = Set.of(sender, Bukkit.getConsoleSender());
		Audience audience = Audience.audience(senders);

		Bukkit.getScheduler().runTaskAsynchronously(mPlugin, () -> {
			final long cpuNanos = System.nanoTime();
			Bukkit.getScheduler().runTask(mPlugin, () -> audience.sendMessage(Component.text(
				"[TreeLoad] " + String.format("%13.9f", (System.nanoTime() - cpuNanos) / 1000000000.0)
					+ " Starting test tree reload")));

			ZoneTreeFactory factory = new ZoneTreeFactory(senders, mNamespaces.values());
			Bukkit.getScheduler().runTask(mPlugin, () -> audience.sendMessage(Component.text(
				"[TreeLoad] " + String.format("%13.9f", (System.nanoTime() - cpuNanos) / 1000000000.0)
					+ " Got fragments")));

			final ZoneTreeBase newTree;
			try {
				newTree = factory.build();
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(mPlugin, () -> {
					audience.sendMessage(Component.text(
						"[TreeLoad] " + String.format("%13.9f", (System.nanoTime() - cpuNanos) / 1000000000.0)
							+ " An exception occurred creating the zone tree:"));
					MessagingUtils.sendStackTrace(senders, ex);
				});
				return;
			}
			Bukkit.getScheduler().runTask(mPlugin, () -> audience.sendMessage(Component.text(
				"[TreeLoad] " + String.format("%13.9f", (System.nanoTime() - cpuNanos) / 1000000000.0)
					+ " Got zone tree")));

			Bukkit.getScheduler().runTask(mPlugin, () -> {
				@Nullable ZoneTreeBase oldTree = mZoneTree;
				mZoneTree = newTree;
				if (oldTree != null) {
					// Force all fragments to consider all locations as outside themselves
					oldTree.invalidate();
				}

				Bukkit.getScheduler().runTask(mPlugin, () -> audience.sendMessage(Component.text(
					"[TreeLoad] " + String.format("%13.9f", (System.nanoTime() - cpuNanos) / 1000000000.0)
						+ " Swapped zone tree; done")));
			});
		});
	}

	// For a given location, return the zones that contain it.
	private Map<String, Zone> getZonesInternal(Vector loc, boolean fallbackZoneLookup) {
		if (fallbackZoneLookup) {
			Map<String, Zone> result = new HashMap<>();
			for (Map.Entry<String, ZoneNamespace> entry : mNamespaces.entrySet()) {
				String namespaceName = entry.getKey();
				ZoneNamespace zoneNamespace = entry.getValue();

				@Nullable Zone zone = zoneNamespace.fallbackGetZone(loc);
				if (zone != null) {
					result.put(namespaceName, zone);
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
			Set<String> namespaceNames = new LinkedHashSet<>(mLastPlayerZones.get(playerUuid).keySet());
			for (String namespaceName : namespaceNames) {
				applyZoneChange(player, namespaceName, null);
			}
		}
		mLastPlayerZoneFragment.remove(playerUuid);
		mLastPlayerZones.remove(playerUuid);
		mTransferringPlayers.remove(playerUuid);
	}

	public void setTransferring(Player player, boolean isTransferring) {
		UUID playerUuid = player.getUniqueId();
		if (isTransferring) {
			mTransferringPlayers.add(playerUuid);
			unregisterPlayer(player);
		} else {
			mTransferringPlayers.remove(playerUuid);
		}
	}

	private void applyFragmentChange(Player player, @Nullable ZoneFragment currentZoneFragment) {
		UUID playerUuid = player.getUniqueId();
		if (currentZoneFragment != null && mTransferringPlayers.contains(playerUuid)) {
			return;
		}

		@Nullable ZoneFragment lastZoneFragment = mLastPlayerZoneFragment.get(playerUuid);

		Map<String, Zone> lastZones = new HashMap<>();
		if (lastZoneFragment != null) {
			lastZones = lastZoneFragment.getParents();
		}

		Map<String, Zone> currentZones = new HashMap<>();
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
			// Zones changed, send an event for each namespace.
			Set<String> mentionedNamespaceNames = new LinkedHashSet<>(lastZones.keySet());
			mentionedNamespaceNames.addAll(currentZones.keySet());
			for (String namespaceName : mentionedNamespaceNames) {
				// Null zones are valid - indicates no zone.
				@Nullable Zone currentZone = currentZones.get(namespaceName);
				applyZoneChange(player, namespaceName, currentZone);
			}
		}
	}

	@SuppressWarnings("ReferenceEquality")
	private void applyZoneChange(Player player, String namespaceName, @Nullable Zone currentZone) {
		UUID playerUuid = player.getUniqueId();
		// Null zones are valid - indicates no zone.
		@Nullable Map<String, Zone> lastZones = mLastPlayerZones.computeIfAbsent(playerUuid, k -> new HashMap<>());

		@Nullable Zone lastZone = lastZones.get(namespaceName);
		if (lastZone == currentZone) {
			// Nothing to do!
			return;
		}

		ZoneChangeEvent zoneEvent = new ZoneChangeEvent(player, namespaceName, lastZone, currentZone);
		Bukkit.getPluginManager().callEvent(zoneEvent);

		Set<String> lastProperties;
		if (lastZone == null) {
			lastProperties = new LinkedHashSet<>();
		} else {
			lastProperties = lastZone.getProperties();
		}

		Set<String> currentProperties;
		if (currentZone == null) {
			currentProperties = new LinkedHashSet<>();
		} else {
			currentProperties = currentZone.getProperties();
		}

		Set<String> removedProperties = new LinkedHashSet<>(lastProperties);
		removedProperties.removeAll(currentProperties);
		for (String property : removedProperties) {
			ZonePropertyChangeEvent event;
			event = new ZonePropertyChangeEvent(player, namespaceName, "!" + property);
			Bukkit.getPluginManager().callEvent(event);
		}

		Set<String> addedProperties = new LinkedHashSet<>(currentProperties);
		addedProperties.removeAll(lastProperties);
		for (String property : addedProperties) {
			ZonePropertyChangeEvent event;
			event = new ZonePropertyChangeEvent(player, namespaceName, property);
			Bukkit.getPluginManager().callEvent(event);
		}

		if (currentZone == null) {
			lastZones.remove(namespaceName);
			if (lastZones.size() == 0) {
				mLastPlayerZones.remove(playerUuid);
			}
		} else {
			lastZones.put(namespaceName, currentZone);
		}
	}

	private void mergeNamespaces() {
		List<ZoneNamespace> namespaces = new ArrayList<>(mNamespaces.values());

		int numNamespaces = namespaces.size();
		for (int i = 0; i < numNamespaces; i++) {
			ZoneNamespace outer = namespaces.get(i);
			for (int j = i + 1; j < numNamespaces; j++) {
				ZoneNamespace inner = namespaces.get(j);
				mergeNamespaces(outer, inner);
			}
		}
	}

	private void mergeNamespaces(ZoneNamespace outerNamespace, ZoneNamespace innerNamespace) {
		for (Zone outerZone : outerNamespace.getZones()) {
			for (Zone innerZone : innerNamespace.getZones()) {
				@Nullable ZoneBase overlap = outerZone.overlappingZone(innerZone);
				if (overlap == null) {
					continue;
				}
				outerZone.splitByOverlap(overlap, innerZone, true);
				innerZone.splitByOverlap(overlap, outerZone);
			}
		}
	}

	/*
	 * sender receives the debug info, player is the target and sees nothing
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
		} else if (fallbackZones == null || !fallbackZones.equals(fastZones)) {
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

	public Collection<ZoneNamespace> getNamespaces() {
		return mNamespaces.values();
	}
}
