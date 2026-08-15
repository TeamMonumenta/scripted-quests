package com.playmonumenta.scriptedquests.zones;

import com.playmonumenta.common.zones.ZoneManager;
import com.playmonumenta.common.zones.ZoneNamespace;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.MMLog;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Nullable;

public class ZoneFileManager {
	// Used to swap the active state when reloading zones
	public static class ZoneState {
		protected final Map<String, ZoneNamespace> mOwnNamespaces = new HashMap<>();
	}

	private static @MonotonicNonNull ZoneFileManager INSTANCE = null;
	static @Nullable BukkitRunnable mAsyncReloadHandler = null;

	private ZoneState mActiveState = new ZoneState();

	private Audience mReloadRequesters = Audience.empty();
	private Set<CommandSender> mQueuedReloadRequesters = new HashSet<>();

	private ZoneFileManager() {
		mQueuedReloadRequesters.add(Bukkit.getConsoleSender());
	}

	public static ZoneFileManager createInstance() {
		INSTANCE = new ZoneFileManager();
		return INSTANCE;
	}

	public static ZoneFileManager getInstance() {
		if (INSTANCE == null) {
			throw new RuntimeException("Attempted to access ZoneManager before initialization");
		}
		return INSTANCE;
	}

	/*
	 * If sender is non-null, it will be sent debugging information
	 *
	 * In the event we have enough zones this takes a while to load:
	 * Reloading after the first startup could use an async load, only pausing long enough to swap
	 * the generated tree.
	 */
	public void reload(Plugin plugin, @Nullable CommandSender sender) {
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
						handleReloads();
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

	private void handleReloads() {
		do {
			doReload();
		} while (!mQueuedReloadRequesters.isEmpty());
	}

	private void doReload() {
		MMLog.debug("[Zone Reload] Begin");
		Plugin plugin = Plugin.getInstance();
		ZoneManager zoneManager = ZoneManager.getInstance();
		mQueuedReloadRequesters.add(Bukkit.getConsoleSender());
		mReloadRequesters = Audience.audience(mQueuedReloadRequesters);
		mQueuedReloadRequesters = new HashSet<>();

		long cpuNanos = System.nanoTime();
		@Nullable ZoneState reloadingState = new ZoneState();
		Set<String> ownOldNamespaces = mActiveState.mOwnNamespaces.keySet();
		Set<String> otherNamespaces = zoneManager.getNamespaceNames();
		otherNamespaces.removeAll(ownOldNamespaces);

		plugin.mZonePropertyGroupManager.reload(plugin, mReloadRequesters);
		Map<String, ZoneNamespace> ownNamespaces = new ZonesReferenceResolver(plugin, mReloadRequesters, otherNamespaces).resolve();

		MMLog.debug("[Zone Reload] " + String.format("%13.9f", (System.nanoTime() - cpuNanos) / 1000000000.0) + "s Loading new data");

		Set<ZoneNamespace> newNamespaces = new HashSet<>();
		Set<ZoneNamespace> replacedNamespaces = new HashSet<>();
		Set<String> removedNamespaces = new HashSet<>();

		ownOldNamespaces.stream().filter(n -> !ownNamespaces.containsKey(n)).forEach(removedNamespaces::add);
		for (ZoneNamespace namespace : ownNamespaces.values()) {
			if (ownOldNamespaces.contains(namespace.getName())) {
				replacedNamespaces.add(namespace);
			} else {
				newNamespaces.add(namespace);
			}
		}

		zoneManager.bulkPluginZoneNamespaceChanges(newNamespaces, replacedNamespaces, removedNamespaces).join();

		mActiveState = reloadingState;

		mReloadRequesters.sendMessage(Component.text("Zone files reloaded successfully.", NamedTextColor.GOLD));
	}
}
