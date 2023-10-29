package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.ZoneProperty;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import com.playmonumenta.scriptedquests.zones.ZonePropertyChangeEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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
		String namespaceName = event.getNamespaceName();
		String property = event.getProperty();

		changeEvent(player, namespaceName, property);
	}

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
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

			return name + ":" + property.getComponents().size();
		});
	}

	private void changeEvent(Player player, String namespaceName, String name) {
		if (namespaceName == null || namespaceName.isEmpty()) {
			return;
		}

		if (name == null || name.isEmpty()) {
			return;
		}

		Map<String, ZoneProperty> namespace = mZoneProperties.get(namespaceName);
		if (namespace == null) {
			return;
		}

		ZoneProperty entry = namespace.get(name);
		if (entry == null) {
			return;
		}

		entry.changeEvent(mPlugin, player);
	}

	public Map<String, Map<String, ZoneProperty>> getZoneProperties() {
		return Collections.unmodifiableMap(mZoneProperties);
	}
}
