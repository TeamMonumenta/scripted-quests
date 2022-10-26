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
		String layer = event.getLayer();
		String property = event.getProperty();

		changeEvent(player, layer, property);
	}

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, @Nullable CommandSender sender) {
		mZoneProperties.clear();

		QuestUtils.loadScriptedQuests(plugin, "zone_properties", sender, (object) -> {
			// Load this file into a ZoneProperty object
			ZoneProperty property = new ZoneProperty(object);
			String layerName = property.getLayer();
			String name = property.getName();

			if (!mZoneProperties.containsKey(layerName)) {
				mZoneProperties.put(layerName, new HashMap<>());
			}
			Map<String, ZoneProperty> layer = mZoneProperties.get(layerName);

			ZoneProperty existingProperty = layer.get(name);
			if (existingProperty != null) {
				// Existing ZoneProperty
				existingProperty.addFromOther(plugin, property);
			} else {
				layer.put(name, property);
			}

			return name + ":" + property.getComponents().size();
		});
	}

	private void changeEvent(Player player, String layerName, String name) {
		if (layerName == null || layerName.isEmpty()) {
			return;
		}

		if (name == null || name.isEmpty()) {
			return;
		}

		Map<String, ZoneProperty> layer = mZoneProperties.get(layerName);
		if (layer == null) {
			return;
		}

		ZoneProperty entry = layer.get(name);
		if (entry == null) {
			return;
		}

		entry.changeEvent(mPlugin, player);
	}

	public Map<String, Map<String, ZoneProperty>> getZoneProperties() {
		return Collections.unmodifiableMap(mZoneProperties);
	}
}
