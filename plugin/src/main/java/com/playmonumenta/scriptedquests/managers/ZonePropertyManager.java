package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.common.event.ZonePropertyChangeEvent;
import com.playmonumenta.common.zones.ZoneManager;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestDeath;
import com.playmonumenta.scriptedquests.quests.QuestLogin;
import com.playmonumenta.scriptedquests.quests.ZoneProperty;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;

public class ZonePropertyManager implements Listener {
	private final Plugin mPlugin;
	private final Map<String, Map<String, ZoneProperty>> mZoneProperties = new HashMap<>();

	public ZonePropertyManager(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void zonePropertyChangeEvent(ZonePropertyChangeEvent event) {
		Player player = event.getPlayer();
		String property = event.getProperty();
		// TEMP: strip ! for now; should remove this eventually.
		// requires change in monumenta-common
		boolean isExiting = property.startsWith("!");
		String name = isExiting ? property.substring(1) : property;

		ZoneProperty prop = lookup(event.getNamespaceName(), name);
		if (prop == null) {
			return;
		}

		if (isExiting) {
			prop.changeExitEvent(mPlugin, player);
			return;
		}

		prop.changeEnterEvent(mPlugin, player);
	}

	public boolean deathEvent(Plugin plugin, PlayerDeathEvent event) {
		Player player = event.getEntity();

		for (Map.Entry<String, Map<String, ZoneProperty>> namespace : mZoneProperties.entrySet()) {
			for (Map.Entry<String, ZoneProperty> prop : namespace.getValue().entrySet()) {
				if (!ZoneManager.getInstance().hasProperty(player.getLocation(), namespace.getKey(), prop.getKey())) {
					continue;
				}

				for (QuestDeath death : prop.getValue().getDeaths()) {
					if (death.deathEvent(plugin, event)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public boolean loginEvent(Plugin plugin, PlayerJoinEvent event) {
		Player player = event.getPlayer();
		boolean isSuccess = false;

		for (Map.Entry<String, Map<String, ZoneProperty>> namespace : mZoneProperties.entrySet()) {
			for (Map.Entry<String, ZoneProperty> prop : namespace.getValue().entrySet()) {
				if (!ZoneManager.getInstance().hasProperty(
					player.getLocation(),
					namespace.getKey(),
					prop.getKey()
			)) {
					continue;
				}

				for (QuestLogin login : prop.getValue().getLogins()) {
					if (login.loginEvent(plugin, event)) {
						isSuccess = true;
					}
				}
			}
		}

		return isSuccess;
	}

	/* If sender is non-null, it will be sent debugging information */
	public void reload(Plugin plugin, @Nullable CommandSender sender) {
		mZoneProperties.clear();

		QuestUtils.loadScriptedQuests(plugin, "zone_properties", sender, (object) -> {
			// Load this file into a ZoneProperty object
			ZoneProperty property = new ZoneProperty(object);
			String namespaceName = property.getNamespaceName();
			String name = property.getName();

			if (!mZoneProperties.containsKey(namespaceName)) {
				mZoneProperties.put(namespaceName, new HashMap<>());
			}
			Map<String, ZoneProperty> namespace = mZoneProperties.get(namespaceName);

			ZoneProperty existingProperty = namespace.get(name);
			if (existingProperty != null) {
				// Existing ZoneProperty
				existingProperty.addFromOther(plugin, property);
			} else {
				namespace.put(name, property);
			}

			return name + ":" + property.getEnterComponents().size();
		});
	}

	public Map<String, Map<String, ZoneProperty>> getZoneProperties() {
		return Collections.unmodifiableMap(mZoneProperties);
	}

	private @Nullable ZoneProperty lookup(String namespace, String name) {
		if (namespace.isEmpty() || name.isEmpty()) {
			return null;
		}

		Map<String, ZoneProperty> ns = mZoneProperties.get(namespace);

		return ns == null
		? null
		: ns.get(name);
	}
}
