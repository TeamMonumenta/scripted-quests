package com.playmonumenta.scriptedquests.zones;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.ArgUtils;
import com.playmonumenta.scriptedquests.utils.MMLog;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class ZoneManager {
	// Used to swap the active state when reloading zones
	private static class ZoneState {
		protected final Map<String, ZoneNamespace> mNamespaces = new HashMap<>();
		protected @MonotonicNonNull WorldRegexMatcher mWorldRegexMatcher = null;
		protected @MonotonicNonNull ZoneTreeBase mZoneTree = null;
	}

	private static final String[] SUGGESTIONS_EXECUTE_FALLBACK = {"\"Suggestions unavailable through /execute\""};

	private final Plugin mPlugin;
	private static @MonotonicNonNull ZoneManager INSTANCE = null;
	static @MonotonicNonNull BukkitRunnable mPlayerTracker = null;
	static @Nullable BukkitRunnable mAsyncReloadHandler = null;

	private @Nullable ZoneState mReloadingState = null;
	private ZoneState mActiveState = new ZoneState();
	private final Map<String, ZoneNamespace> mPluginNamespaces = new HashMap<>();

	private final Set<UUID> mTransferringPlayers = new HashSet<>();
	private final Map<UUID, ZoneFragment> mLastPlayerZoneFragment = new HashMap<>();
	private final Map<UUID, Map<String, Zone>> mLastPlayerZones = new HashMap<>();

	private Audience mReloadRequesters = Audience.empty();
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
	 * Returns the WorldRegexMatcher; will never return null as long as the plugin loaded successfully
	 */
	public WorldRegexMatcher getWorldRegexMatcher() {
		WorldRegexMatcher matcher = mActiveState.mWorldRegexMatcher;
		if (matcher == null) {
			throw new RuntimeException("WorldRegexMatcher unavailable before ScriptedQuests finishes loading");
		}
		return matcher;
	}

	/*
	 * Returns all ZoneFragments that overlap a bounding box.
	 */
	public Set<ZoneFragment> getZoneFragments(BoundingBox bb) {
		return mActiveState.mZoneTree.getZoneFragments(bb);
	}

	/*
	 * For a given location, return the fragment that contains it.
	 * Returns null if no fragment overlaps it.
	 *
	 * If fallback zone lookup is enabled, this should be avoided.
	 */
	public @Nullable ZoneFragment getZoneFragment(Vector loc) {
		return mActiveState.mZoneTree.getZoneFragment(loc);
	}

	public @Nullable ZoneFragment getZoneFragment(Location loc) {
		if (loc == null) {
			return null;
		}

		return mActiveState.mZoneTree.getZoneFragment(loc.toVector());
	}

	// For a given location, return the zones that contain it.
	public Map<String, Zone> getZones(Location loc) {
		return getZonesInternal(loc, mPlugin.mFallbackZoneLookup);
	}

	/*
	 * Returns all zones that overlap a bounding box, optionally including eclipsed zones.
	 */
	public Set<Zone> getZones(World world, BoundingBox bb, boolean includeEclipsed) {
		return mActiveState.mZoneTree.getZones(world, bb, includeEclipsed);
	}

	/*
	 * For a given namespace and location, return the zone that
	 * contains it. Returns null if no zone overlaps it.
	 */
	public @Nullable Zone getZone(Location loc, String namespaceName) {
		if (mPlugin.mFallbackZoneLookup) {
			@Nullable ZoneNamespace zoneNamespace = mActiveState.mNamespaces.get(namespaceName);
			if (zoneNamespace == null) {
				return null;
			}
			return zoneNamespace.fallbackGetZone(loc);
		} else {
			return mActiveState.mZoneTree.getZone(loc, namespaceName);
		}
	}

	public boolean hasProperty(Location loc, String namespaceName, String propertyName) {
		@Nullable Zone zone = getZone(loc, namespaceName);
		if (zone == null) {
			return false;
		}
		return zone.hasProperty(propertyName);
	}

	// Passing a player is optimized to use the last known location
	public boolean hasProperty(Player player, String namespaceName, String propertyName) {
		if (player == null) {
			return false;
		}
		UUID playerUuid = player.getUniqueId();

		@Nullable Map<String, Zone> lastZones = mLastPlayerZones.get(playerUuid);
		if (lastZones == null) {
			return false;
		}

		@Nullable Zone lastZone = lastZones.get(namespaceName);
		if (lastZone == null) {
			return false;
		}

		return lastZone.hasProperty(propertyName);
	}

	public Set<String> getNamespaceNames() {
		return new HashSet<>(mActiveState.mNamespaces.keySet());
	}

	public String[] getNamespaceNameSuggestions() {
		return ArgUtils.quoteIfNeeded(new TreeSet<>(getNamespaceNames()));
	}

	public static ArgumentSuggestions getNamespaceArgumentSuggestions() {
		return ArgumentSuggestions.strings(info -> getInstance().getNamespaceNameSuggestions());
	}

	public Set<String> getLoadedProperties(String namespaceName) {
		@Nullable ZoneNamespace namespace = mActiveState.mNamespaces.get(namespaceName);
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
		sender.sendMessage(Component.text("Zone reload started in the background, you will be notified of progress.", NamedTextColor.GOLD));
		if (mAsyncReloadHandler == null) {
			// Start a new async task to handle reloads
			mAsyncReloadHandler = new BukkitRunnable() {
				@Override
				public void run() {
					try {
						handleReloads(plugin);
					} catch (Exception e) {
						Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
							MessagingUtils.sendStackTrace(mReloadRequesters, e);
							mReloadRequesters.sendMessage(Component.text("Zones failed to reload.", NamedTextColor.RED));
						});
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
		doReload(plugin, false);
	}

	public void doReload(Plugin plugin, boolean firstRun) {
		MMLog.fine("[Zone Reload] Begin");
		mQueuedReloadRequesters.add(Bukkit.getConsoleSender());
		mReloadRequesters = Audience.audience(mQueuedReloadRequesters);
		mQueuedReloadRequesters = new HashSet<>();

		long cpuNanos = System.nanoTime();
		mReloadingState = new ZoneState();
		ZoneNamespace.clearDynmapLayers();
		MMLog.fine("[Zone Reload] " + String.format("%13.9f", (System.nanoTime() - cpuNanos) / 1000000000.0) + "s Resetting old data");

		cpuNanos = System.nanoTime();
		plugin.mZonePropertyGroupManager.reload(plugin, mReloadRequesters);

		// Refresh plugin namespaces
		if (mPlugin.mShowZonesDynmap) {
			for (ZoneNamespace namespace : mPluginNamespaces.values()) {
				namespace.refreshDynmapLayer();
			}
		}

		mReloadingState.mNamespaces.putAll(mPluginNamespaces);
		mReloadingState.mNamespaces.putAll(
			new ZonesReferenceResolver(plugin, mReloadRequesters, mReloadingState.mNamespaces.keySet()).resolve());
		MMLog.fine("[Zone Reload] " + String.format("%13.9f", (System.nanoTime() - cpuNanos) / 1000000000.0) + "s Loading new data");

		final Set<String> worldRegexes = new HashSet<>();
		for (ZoneNamespace zoneNamespace : mReloadingState.mNamespaces.values()) {
			for (Zone zone : zoneNamespace.getZones()) {
				worldRegexes.add(zone.getWorldRegex());
			}
		}

		// If not the first run, start this async and await it later
		CompletableFuture<Void> worldMatcherFuture = new CompletableFuture<>();
		if (firstRun) {
			mReloadingState.mWorldRegexMatcher = new WorldRegexMatcher(worldRegexes);
			worldMatcherFuture.complete(null);
		} else {
			Bukkit.getScheduler().scheduleSyncDelayedTask(mPlugin, () -> {
				// Prevent the state from changing out from under us while we wait for sync code to be available
				ZoneState reloadingState = mReloadingState;
				reloadingState.mWorldRegexMatcher = new WorldRegexMatcher(worldRegexes);
				// Mark as complete
				worldMatcherFuture.complete(null);
			});
		}

		mReloadRequesters.sendMessage(Component.text("Zone parsing successful, optimizing before enabling...", NamedTextColor.GOLD));

		// Gather all zone fragments
		ZoneTreeFactory factory = new ZoneTreeFactory(mReloadRequesters, mReloadingState.mNamespaces.values());

		// Create the new tree. This could take a long time with enough fragments.
		try {
			mReloadingState.mZoneTree = factory.build();
		} catch (Exception ex) {
			mReloadingState = null;
			Bukkit.getScheduler().runTask(mPlugin, () -> MessagingUtils.sendStackTrace(mReloadRequesters, ex));
			return;
		}

		if (mPlugin.mShowZonesDynmap) {
			mReloadingState.mZoneTree.refreshDynmapTree();
		}
		mReloadRequesters.sendMessage(Component.text(
			"Zone tree dev stats - fragments: "
				+ mReloadingState.mZoneTree.fragmentCount()
				+ ", max depth: "
				+ mReloadingState.mZoneTree.maxDepth()
				+ ", ave depth: "
				+ String.format("%.2f", mReloadingState.mZoneTree.averageDepth()),
			NamedTextColor.GOLD
		));

		// Make sure only one player tracker runs at a time.
		if (mPlayerTracker != null && !mPlayerTracker.isCancelled()) {
			mPlayerTracker.cancel();
		}

		worldMatcherFuture.join();

		// Swap the tree out; this is really fast!
		@Nullable ZoneTreeBase oldTree = mActiveState.mZoneTree;
		mActiveState = mReloadingState;
		mReloadingState = null;
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
						Map<String, Zone> currentZones = getZones(playerLocation);
						// Need to check all namespace names, not just the ones the player is in
						for (String namespaceName : mReloadingState.mNamespaces.keySet()) {
							Zone currentZone = currentZones.get(namespaceName);
							// Handles comparing to previous zone if needed
							applyZoneChange(player, namespaceName, currentZone);
						}
						continue;
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

		mReloadRequesters.sendMessage(Component.text("Zones reloaded successfully.", NamedTextColor.GOLD));
	}

	// For a given location, return the zones that contain it.
	private Map<String, Zone> getZonesInternal(Location loc, boolean fallbackZoneLookup) {
		if (fallbackZoneLookup) {
			Map<String, Zone> result = new HashMap<>();
			for (Map.Entry<String, ZoneNamespace> entry : mActiveState.mNamespaces.entrySet()) {
				String namespaceName = entry.getKey();
				ZoneNamespace zoneNamespace = entry.getValue();

				@Nullable Zone zone = zoneNamespace.fallbackGetZone(loc);
				if (zone != null) {
					result.put(namespaceName, zone);
				}
			}
			return result;
		} else {
			return mActiveState.mZoneTree.getZones(loc);
		}
	}

	public void onLoadWorld(World world) {
		ZoneState tempState;
		WorldRegexMatcher tempMatcher;

		// Nullable, can change async
		tempState = mReloadingState;
		if (tempState != null) {
			tempMatcher = tempState.mWorldRegexMatcher;
			if (tempMatcher != null) {
				tempMatcher.onLoadWorld(world);
			}
		}

		// Never null, but can change async
		tempState = mActiveState;
		tempMatcher = tempState.mWorldRegexMatcher;
		if (tempMatcher != null) {
			tempMatcher.onLoadWorld(world);
		}
	}

	public void onUnloadWorld(World world) {
		ZoneState tempState;
		WorldRegexMatcher tempMatcher;

		// Nullable, can change async
		tempState = mReloadingState;
		if (tempState != null) {
			tempMatcher = tempState.mWorldRegexMatcher;
			if (tempMatcher != null) {
				tempMatcher.onUnloadWorld(world);
			}
		}

		// Never null, but can change async
		tempState = mActiveState;
		tempMatcher = tempState.mWorldRegexMatcher;
		if (tempMatcher != null) {
			tempMatcher.onUnloadWorld(world);
		}
	}

	public void unregisterPlayer(Player player) {
		UUID playerUuid = player.getUniqueId();
		applyFragmentChange(player, null);
		if (mPlugin.mFallbackZoneLookup && mLastPlayerZones.containsKey(playerUuid)) {
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
		World world = player.getWorld();
		if (currentZoneFragment != null && mTransferringPlayers.contains(playerUuid)) {
			return;
		}

		Map<String, Zone> lastZones = mLastPlayerZones.get(playerUuid);

		Map<String, Zone> currentZones = new HashMap<>();
		if (currentZoneFragment != null) {
			currentZones = currentZoneFragment.getParents(world);
		}

		// We've already confirmed the player changed zone fragments; null is valid.
		mLastPlayerZoneFragment.put(playerUuid, currentZoneFragment);

		if (currentZones.equals(lastZones)) {
			// If the zones are identical between both fragments, nothing more to do.
			return;
		}

		if (!mPlugin.mFallbackZoneLookup) {
			// Zones changed, send an event for each namespace.
			Set<String> mentionedNamespaceNames = new LinkedHashSet<>(currentZones.keySet());
			if (lastZones != null) {
				mentionedNamespaceNames.addAll(lastZones.keySet());
			}
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
			if (lastZones.isEmpty()) {
				mLastPlayerZones.remove(playerUuid);
			}
		} else {
			lastZones.put(namespaceName, currentZone);
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
		Location playerLocation = player.getLocation();

		sender.sendMessage(Component.text("Cached player info according to zone fragment tree:"));

		@Nullable ZoneFragment cachedFragment = mLastPlayerZoneFragment.get(playerUuid);
		if (cachedFragment == null) {
			sender.sendMessage(Component.text("Target is not in any zone."));
		} else {
			sender.sendMessage(Component.text(cachedFragment.toString()));

			Map<String, Zone> fragmentParents = mLastPlayerZones.get(playerUuid);
			if (fragmentParents == null || fragmentParents.isEmpty()) {
				sender.sendMessage(Component.text("Fragment has no parent zones! Where did it come from?"));
			} else {
				sender.sendMessage(Component.text("Fragment parents:"));
				for (Zone zone : fragmentParents.values()) {
					sender.sendMessage(Component.text(zone.toString()));
				}
			}
			sender.sendMessage(Component.text("Fragment parents and eclipsed:"));
			for (List<Zone> zones : cachedFragment.getParentsAndEclipsed().values()) {
				for (Zone zone : zones) {
					sender.sendMessage(Component.text(zone.toString()));
				}
			}
		}

		if (mPlugin.mFallbackZoneLookup) {
			sender.sendMessage(Component.text("Cached player info according to slow/reliable method:"));

			@Nullable Map<String, Zone> cachedZones = mLastPlayerZones.get(playerUuid);
			if (cachedZones == null) {
				sender.sendMessage(Component.text("Target is not in any zone."));
			} else {
				if (cachedZones.isEmpty()) {
					sender.sendMessage(Component.text("Target is not in any zone, but an empty map is left over."));
				}
				for (Zone cachedZone : cachedZones.values()) {
					sender.sendMessage(Component.text(cachedZone.toString()));
				}
			}
		}

		sender.sendMessage(Component.text("Uncached location info:"));
		sendDebug(sender, playerLocation);
	}

	public void sendDebug(CommandSender sender, Location loc) {
		if (sender == null) {
			return;
		}

		@Nullable Map<String, Zone> fallbackZones = getZonesInternal(loc, true);
		@Nullable Map<String, Zone> fastZones = getZonesInternal(loc, false);
		if (fallbackZones == null && fastZones == null) {
			sender.sendMessage(Component.text("Fast lookup matches slow/reliable lookup (both null)"));
		} else if (fallbackZones == null || !fallbackZones.equals(fastZones)) {
			sender.sendMessage(Component.text("Fast lookup DOES NOT match slow/reliable lookup"));

			sender.sendMessage(Component.text("Slow/reliable lookup version:"));
			if (fallbackZones == null) {
				sender.sendMessage(Component.text("Target is not in any zone."));
			} else {
				if (fallbackZones.isEmpty()) {
					sender.sendMessage(Component.text("Target is not in any zone, but an empty map is left over."));
				}
				for (Zone fallbackZone : fallbackZones.values()) {
					sender.sendMessage(Component.text(fallbackZone.toString()));
				}
			}
		} else {
			sender.sendMessage(Component.text("Fast lookup matches slow/reliable lookup (both exist and are equal)"));
		}

		sender.sendMessage(Component.text("Fast lookup version:"));
		@Nullable ZoneFragment fragment = getZoneFragment(loc);
		if (fragment == null) {
			sender.sendMessage(Component.text("Target is not in any zone."));
			return;
		}

		if (!fragment.within(loc.toVector())) {
			sender.sendMessage(Component.text("Target is not in the zone fragment they were reported in."));
		}
		sender.sendMessage(Component.text(fragment.toString()));

		Map<String, Zone> fragmentParents = fragment.getParents(loc.getWorld());
		if (fragmentParents.isEmpty()) {
			sender.sendMessage(Component.text("Fragment has no parent zones! Where did it come from?"));
		}
		sender.sendMessage(Component.text("Fragment parents:"));
		for (Zone zone : fragmentParents.values()) {
			sender.sendMessage(Component.text(zone.toString()));
		}
		sender.sendMessage(Component.text("Fragment parents and eclipsed:"));
		for (List<Zone> zones : fragment.getParentsAndEclipsed().values()) {
			for (Zone zone : zones) {
				sender.sendMessage(Component.text(zone.toString()));
			}
		}
	}

	public Collection<ZoneNamespace> getNamespaces() {
		return mActiveState.mNamespaces.values();
	}
}
