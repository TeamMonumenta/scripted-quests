package com.playmonumenta.scriptedquests.managers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.common.event.ZonePropertyChangeEvent;
import com.playmonumenta.common.event.ZonePropertyChangeEvent.Change;
import com.playmonumenta.common.zones.Zone;
import com.playmonumenta.common.zones.ZoneFragment;
import com.playmonumenta.common.zones.ZoneManager;
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
import java.util.TreeMap;
import java.util.function.Function;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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
 * Death and login rules are evaluated in two groups: the unscoped rules in death and login,
 * which apply everywhere, and the rules of the properties that are active at the player's
 * location. Both groups are ordered by priority, then by namespace, property name and array
 * index. Death stops at the first match, login runs every match.
 */
public class ZoneBehaviorCoordinator implements Listener {
	private final Plugin mPlugin;
	private final Map<String, Map<String, ZoneProperty>> mZoneProperties = new HashMap<>();
	private final List<QuestDeath> mUnscopedDeaths = new ArrayList<>();
	private final List<QuestLogin> mUnscopedLogins = new ArrayList<>();

	public ZoneBehaviorCoordinator(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void zonePropertyChangeEvent(ZonePropertyChangeEvent event) {
		ZoneProperty property = lookup(event.getNamespaceName(), event.getPropertyName());
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
		List<QuestDeath> candidates = withActiveRules(event.getEntity(), mUnscopedDeaths, ZoneProperty::getDeaths);
		candidates.sort(Comparator.comparingInt(QuestDeath::getPriority));

		for (QuestDeath death : candidates) {
			if (death.deathEvent(mPlugin, event)) {
				return true;
			}
		}

		return false;
	}

	/* Returns true if any rule matched, false otherwise */
	public boolean loginEvent(PlayerJoinEvent event) {
		boolean isSuccess = false;
		List<QuestLogin> candidates = withActiveRules(event.getPlayer(), mUnscopedLogins, ZoneProperty::getLogins);
		candidates.sort(Comparator.comparingInt(QuestLogin::getPriority));

		for (QuestLogin login : candidates) {
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

		rebuildUnscoped(plugin, sender);
	}

	/*
	 * Rules that are not tied to a property stay in the death and login folders. They are
	 * candidates just like property rules: they fire on their own prerequisites alone.
	 */
	private void loadUnscopedRules(Plugin plugin, @Nullable CommandSender sender) {
		QuestUtils.loadScriptedQuests(plugin, "death", sender, (object, file) -> {
			mUnscopedDeaths.add(new QuestDeath(object, priorityOf(object, file)));
			return null;
		});

		QuestUtils.loadScriptedQuests(plugin, "login", sender, (object, file) -> {
			mUnscopedLogins.add(new QuestLogin(object, priorityOf(object, file)));
			return null;
		});
	}

	private static int priorityOf(JsonObject object, File file) {
		JsonElement priority = object.get("priority");
		if (priority == null) {
			MMLog.warning("Unscoped rule '" + file.getPath() + "' has no priority, it will be evaluated last");
			return Integer.MAX_VALUE;
		}

		return priority.getAsInt();
	}

	private void rebuildUnscoped(Plugin plugin, @Nullable CommandSender sender) {
		mUnscopedDeaths.clear();
		mUnscopedLogins.clear();

		loadUnscopedRules(plugin, sender);
	}

	/*
	 * Returns the unscoped rules followed by the rules of every property that is active at
	 * the player's location. The zone tree resolves the location once and returns the
	 * winning zone of each namespace, so an eclipsed zone never contributes.
	 */
	private <T> List<T> withActiveRules(Player player, List<T> unscoped, Function<ZoneProperty, List<T>> rules) {
		List<T> candidates = new ArrayList<>(unscoped);

		ZoneFragment fragment = ZoneManager.getInstance().getZoneFragment(player.getLocation());
		if (fragment == null) {
			return candidates;
		}

		Map<String, Zone> zones = new TreeMap<>(fragment.getParents(player.getWorld()));
		for (Map.Entry<String, Zone> zone : zones.entrySet()) {
			List<String> names = new ArrayList<>(zone.getValue().getProperties());
			names.sort(Comparator.naturalOrder());

			for (String name : names) {
				ZoneProperty property = lookup(zone.getKey(), name);
				if (property != null) {
					candidates.addAll(rules.apply(property));
				}
			}
		}

		return candidates;
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
