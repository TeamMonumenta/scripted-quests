package com.playmonumenta.scriptedquests.managers;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.ZoneProperty;
import com.playmonumenta.scriptedquests.utils.QuestUtils;

public class ZonePropertyManager {
	private final HashMap<String, HashMap<String, ZoneProperty>> mZoneProperties = new HashMap<String, HashMap<String, ZoneProperty>>();

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, CommandSender sender) {
		mZoneProperties.clear();

		QuestUtils.loadScriptedQuests(plugin, "zone_properties", sender, (object) -> {
			// Load this file into a NpcTrader object
			ZoneProperty property = new ZoneProperty(object);
			String layerName = property.getLayer();
			String name = property.getName();

			if (!mZoneProperties.containsKey(layerName)) {
				mZoneProperties.put(layerName, new HashMap<String, ZoneProperty>());
			}
			HashMap<String, ZoneProperty> layer = mZoneProperties.get(layerName);

			if (layer.containsKey(name)) {
				throw new Exception(name + "' already exists on layer '" + layerName + "'!");
			}

			layer.put(name, property);

			return name + ":" + Integer.toString(property.getComponents().size());
		});
	}

	public void changeEvent(Plugin plugin, Player player, String layerName, String name) {
		if (layerName == null || layerName.isEmpty()) {
			player.sendMessage(ChatColor.RED + "Invalid zone property layerName");
			return;
		}

		if (name == null || name.isEmpty()) {
			player.sendMessage(ChatColor.RED + "Invalid zone property name");
			return;
		}

		HashMap<String, ZoneProperty> layer = mZoneProperties.get(layerName);
		if (layer == null) {
			player.sendMessage(ChatColor.RED + "No zone property matching layer '" + layerName + "'");
			return;
		}

		ZoneProperty entry = layer.get(name);
		if (entry == null) {
			player.sendMessage(ChatColor.RED + "No zone property matching '" + name + "' in layer '" + layerName + "'");
			return;
		}

		entry.changeEvent(plugin, player);
	}
}
