package com.playmonumenta.scriptedquests.managers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.common.event.ZonePropertyChangeEvent;
import com.playmonumenta.common.event.ZonePropertyChangeEvent.Change;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestDeath;
import com.playmonumenta.scriptedquests.quests.QuestLogin;
import com.playmonumenta.scriptedquests.quests.ZoneProperty;
import com.playmonumenta.scriptedquests.utils.MMLog;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;

/*
 * The single owner of zone behavior dispatch.
 *
 * Enter and exit are routed by (namespace, name) from the zone property change event.
 * Death and login rules are flattened out of every loaded file and evaluated by their own
 * prerequisites alone - never by the file they came from. Death stops at the first match,
 * login runs every match.
 */
public class ZoneBehaviorCoordinator implements Listener {
	private final Plugin mPlugin;
	private final Map<String, Map<String, ZoneProperty>> mZoneProperties = new HashMap<>();
	private final List<QuestDeath> mDeaths = new ArrayList<>();
	private final List<QuestLogin> mLogins = new ArrayList<>();

	public ZoneBehaviorCoordinator(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void zonePropertyChangeEvent(ZonePropertyChangeEvent event) {
		ZoneProperty property = lookup(event.getNamespaceName(), event.getProperty());
		if (property == null) {
			return;
		}

		if (event.getChange() == Change.REMOVED) {
			property.changeExitEvent(mPlugin, event.getPlayer());
		} else {
			property.changeEnterEvent(mPlugin, event.getPlayer());
		}
	}

	/* Returns true if a rule matched, false otherwise */
	public boolean deathEvent(PlayerDeathEvent event) {
		for (QuestDeath death : mDeaths) {
			if (death.deathEvent(mPlugin, event)) {
				return true;
			}
		}

		return false;
	}

	/* Returns true if any rule matched, false otherwise */
	public boolean loginEvent(PlayerJoinEvent event) {
		boolean isSuccess = false;

		for (QuestLogin login : mLogins) {
			if (login.loginEvent(mPlugin, event)) {
				isSuccess = true;
			}
		}

		return isSuccess;
	}

	/* If sender is non-null, it will be sent debugging information */
	public void reload(Plugin plugin, @Nullable CommandSender sender) {
		mZoneProperties.clear();

		QuestUtils.loadScriptedQuests(plugin, "zone_properties", sender, (object) -> {
			ZoneProperty property = new ZoneProperty(object);
			String namespaceName = property.getNamespaceName();
			String name = property.getName();

			Map<String, ZoneProperty> namespace = mZoneProperties.computeIfAbsent(namespaceName, key -> new HashMap<>());
			ZoneProperty existingProperty = namespace.get(name);
			if (existingProperty != null) {
				existingProperty.addFromOther(plugin, property);
			} else {
				namespace.put(name, property);
			}

			return name + ":" + property.getComponentCount();
		});

		rebuildCandidates(plugin, sender);
	}

	/*
	 * Rules that are not tied to a property live in global/deaths and global/logins. They are
	 * candidates just like property rules: they fire on their own prerequisites alone.
	 */
	private void loadGlobalRules(Plugin plugin, @Nullable CommandSender sender) {
		QuestUtils.loadScriptedQuests(plugin, "global/deaths", sender, (object, file) -> {
			mDeaths.add(new QuestDeath(object, priorityOf(object, file)));
			return null;
		});

		QuestUtils.loadScriptedQuests(plugin, "global/logins", sender, (object, file) -> {
			mLogins.add(new QuestLogin(object, priorityOf(object, file)));
			return null;
		});
	}

	private static int priorityOf(JsonObject object, File file) {
		JsonElement priority = object.get("priority");
		if (priority == null) {
			MMLog.warning("Global rule '" + file.getPath() + "' has no priority, it will be evaluated last");
			return Integer.MAX_VALUE;
		}

		return priority.getAsInt();
	}

	/*
	 * Candidates are ordered by priority, then by container key and array index, so the result
	 * never depends on map iteration order.
	 */
	private void rebuildCandidates(Plugin plugin, @Nullable CommandSender sender) {
		mDeaths.clear();
		mLogins.clear();

		loadGlobalRules(plugin, sender);

		List<Map.Entry<String, Map<String, ZoneProperty>>> namespaces = new ArrayList<>(mZoneProperties.entrySet());
		namespaces.sort(Map.Entry.comparingByKey());
		for (Map.Entry<String, Map<String, ZoneProperty>> namespace : namespaces) {
			List<Map.Entry<String, ZoneProperty>> properties = new ArrayList<>(namespace.getValue().entrySet());
			properties.sort(Map.Entry.comparingByKey());
			for (Map.Entry<String, ZoneProperty> property : properties) {
				mDeaths.addAll(property.getValue().getDeaths());
				mLogins.addAll(property.getValue().getLogins());
			}
		}

		mDeaths.sort(Comparator.comparingInt(QuestDeath::getPriority));
		mLogins.sort(Comparator.comparingInt(QuestLogin::getPriority));
	}

	public Map<String, Map<String, ZoneProperty>> getZoneProperties() {
		return Collections.unmodifiableMap(mZoneProperties);
	}

	private @Nullable ZoneProperty lookup(String namespace, String name) {
		if (namespace == null || namespace.isEmpty() || name == null || name.isEmpty()) {
			return null;
		}

		Map<String, ZoneProperty> ns = mZoneProperties.get(namespace);

		return ns == null ? null : ns.get(name);
	}
}
