package com.playmonumenta.scriptedquests.quests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;

/*
 * A ZoneProperty is a container for an interaction that occurs when crossing between
 * zones with different properties. Properties can have any name, though # and ! have
 * special meaning when used as the first character - # is used for groups of
 * properties, and ! is used to specify the removal of a property.
 *
 * Let's say you want an area that is in adventure mode, with the property name
 * "adventure mode". Entering ("adventure mode") would run `gamemode @S adventure`,
 * and leaving ("!adventure mode") would run `gamemode @S survival`.
 *
 * Note that you may specify just the "property added" quest components, just the
 * "property removed" quest components, both, or niether. You may also hook plugin
 * code of your own into this system for additional features.
 */
public class ZoneProperty {
	private final ArrayList<QuestComponent> mComponents = new ArrayList<QuestComponent>();
	private final String mLayer;
	private final String mName;
	private final String mDisplayName;

	public ZoneProperty(JsonObject object) throws Exception {
		//////////////////////////////////////// layer (Required) ////////////////////////////////////////
		JsonElement layer = object.get("layer");
		if (layer == null) {
			throw new Exception("'layer' entry is required");
		}
		if (layer.getAsString() == null || layer.getAsString().isEmpty()) {
			throw new Exception("Failed to parse 'layer' as string");
		}
		mLayer = layer.getAsString();

		//////////////////////////////////////// name (Required) ////////////////////////////////////////
		JsonElement name = object.get("name");
		if (name == null) {
			throw new Exception("'name' entry is required");
		}
		if (name.getAsString() == null || name.getAsString().isEmpty()) {
			throw new Exception("Failed to parse 'name' as string");
		}
		mName = name.getAsString();

		//////////////////////////////////////// display_name (Optional) ////////////////////////////////////////
		// Read the optional display name - default to empty string
		JsonElement displayName = object.get("display_name");
		if (displayName == null || displayName.getAsString() == null) {
			mDisplayName = "";
		} else {
			mDisplayName = displayName.getAsString();
		}

		//////////////////////////////////////// quest_components (Required) ////////////////////////////////////////
		JsonElement questComponents = object.get("quest_components");
		if (questComponents == null) {
			throw new Exception("'quest_components' entry is required");
		}
		JsonArray array = questComponents.getAsJsonArray();
		if (array == null) {
			throw new Exception("Failed to parse 'quest_components' as JSON array");
		}

		Iterator<JsonElement> iter = array.iterator();
		while (iter.hasNext()) {
			JsonElement entry = iter.next();

			// TODO: Refactor so that components only require a linkage to the top-level item, not a name/entity type
			mComponents.add(new QuestComponent(mName, mDisplayName, EntityType.PLAYER, entry));
		}

		//////////////////////////////////////// Fail if other keys exist ////////////////////////////////////////
		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("layer") && !key.equals("name") && !key.equals("display_name")
				&& !key.equals("quest_components")) {
				throw new Exception("Unknown quest key: " + key);
			}
		}
	}

	public void addFromOther(Plugin plugin, ZoneProperty other) {
		if (!mLayer.equals(other.mLayer)
		    || !mName.equals(other.mName)) {
			plugin.getLogger().severe("Attempted to add two ZoneProperty objects of different properties!");
			return;
		}
		for (QuestComponent component : other.getComponents()) {
			mComponents.add(component);
		}
	}

	public String getLayer() {
		return mLayer;
	}

	public String getName() {
		return mName;
	}

	public ArrayList<QuestComponent> getComponents() {
		return mComponents;
	}

	public void changeEvent(Plugin plugin, Player player) {
		for (QuestComponent component : mComponents) {
			component.doActionsIfPrereqsMet(plugin, player, null);
		}
	}
}
