package com.playmonumenta.scriptedquests.quests;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import com.playmonumenta.scriptedquests.quests.components.QuestComponentList;
import com.playmonumenta.scriptedquests.zones.event.ZoneBlockBreakEvent;
import com.playmonumenta.scriptedquests.zones.event.ZoneBlockInteractEvent;
import com.playmonumenta.scriptedquests.zones.event.ZoneEvent;
import com.playmonumenta.scriptedquests.zones.event.ZoneRemoteClickEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

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
	private final String mLayer;
	private final String mName;
	private final String mDisplayName;
	private final QuestComponentList mComponents = new QuestComponentList();
	private final List<ZoneEvent> mEvents = new ArrayList<>();

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

		for (JsonElement entry : array) {
			// TODO: Refactor so that components only require a linkage to the top-level item, not a name/entity type
			mComponents.add(new QuestComponent(mName, mDisplayName, EntityType.PLAYER, entry));
		}

		//////////////////////////////////////// events (optional) ////////////////////////////////////////
		JsonObject eventsObject = object.getAsJsonObject("events");
		if (eventsObject != null) {
			JsonArray blockBreakEvents = eventsObject.getAsJsonArray("block_break");
			if (blockBreakEvents != null) {
				for (JsonElement blockBreakEvent : blockBreakEvents) {
					mEvents.add(ZoneBlockBreakEvent.fromJson(blockBreakEvent));
				}
			}
			JsonArray blockInteractEvents = eventsObject.getAsJsonArray("block_interact");
			if (blockInteractEvents != null) {
				for (JsonElement blockInteractEvent : blockInteractEvents) {
					mEvents.add(ZoneBlockInteractEvent.fromJson(blockInteractEvent));
				}
			}
			JsonArray remoteClickEvents = eventsObject.getAsJsonArray("remote_click");
			if (remoteClickEvents != null) {
				for (JsonElement remoteClickEvent : remoteClickEvents) {
					mEvents.add(ZoneRemoteClickEvent.fromJson(remoteClickEvent));
				}
			}
		}

		//////////////////////////////////////// Fail if other keys exist ////////////////////////////////////////
		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("layer") && !key.equals("name") && !key.equals("display_name")
				    && !key.equals("quest_components") && !key.equals("events")) {
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

	public List<QuestComponent> getComponents() {
		return mComponents.getComponents();
	}

	public void changeEvent(Plugin plugin, Player player) {
		mComponents.run(new QuestContext(plugin, player, null));
	}

	@SuppressWarnings("unchecked")
	public <T> Collection<? extends T> getEvents(Class<T> eventClass) {
		return (Collection<? extends T>) mEvents.stream().filter(eventClass::isInstance).collect(Collectors.toList());
	}
}
