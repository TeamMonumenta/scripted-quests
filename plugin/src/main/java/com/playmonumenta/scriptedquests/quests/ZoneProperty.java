package com.playmonumenta.scriptedquests.quests;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import com.playmonumenta.scriptedquests.quests.components.QuestComponentList;
import com.playmonumenta.scriptedquests.utils.JsonUtils;
import com.playmonumenta.scriptedquests.utils.MMLog;
import com.playmonumenta.scriptedquests.zones.event.ZoneBlockBreakEvent;
import com.playmonumenta.scriptedquests.zones.event.ZoneBlockInteractEvent;
import com.playmonumenta.scriptedquests.zones.event.ZoneEntityDeathEvent;
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
 * "property removed" quest components, both, or neither. You may also hook plugin
 * code of your own into this system for additional features.
 */
public class ZoneProperty {
	public enum EventTypes {
		ENTER_EVENT,
		EXIT_EVENT,
	}

	private final String mNamespaceName;
	private final String mName;
	private final String mDisplayName;
	private final QuestComponentList mEnterComponents = new QuestComponentList();
	private final QuestComponentList mExitComponents = new QuestComponentList();
	private final List<QuestDeath> mDeaths = new ArrayList<>();
	private final List<QuestLogin> mLogins = new ArrayList<>();
	private final List<ZoneEvent> mEvents = new ArrayList<>();

	private static final JsonArray EMPTY_ARRAY = new JsonArray();

	public ZoneProperty(JsonObject object) throws Exception {
		mNamespaceName = JsonUtils.getString(object, "namespace");
		mName = JsonUtils.getString(object, "name");
		if (mNamespaceName.isEmpty() || mName.isEmpty()) {
			throw new Exception("Failed to parse zone property namespace/name as string");
		}
		mDisplayName = JsonUtils.getString(object, "display_name", "");
		// TODO: Refactor so that components only require a linkage to the top-level item, not a name/entity type
		for (JsonElement entry : JsonUtils.getJsonArray(object, "on_enter", EMPTY_ARRAY)) {
			mEnterComponents.add(new QuestComponent(mName, mDisplayName, EntityType.PLAYER, entry));
		}
		for (JsonElement entry : JsonUtils.getJsonArray(object, "on_exit", EMPTY_ARRAY)) {
			mExitComponents.add(new QuestComponent(mName, mDisplayName, EntityType.PLAYER, entry));
		}
		for (JsonElement entry : JsonUtils.getJsonArray(object, "deaths", EMPTY_ARRAY)) {
			mDeaths.add(new QuestDeath(entry.getAsJsonObject()));
		}
		for (JsonElement entry : JsonUtils.getJsonArray(object, "logins", EMPTY_ARRAY)) {
			mLogins.add(new QuestLogin(entry.getAsJsonObject()));
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
			JsonArray entityDeathEvents = eventsObject.getAsJsonArray("entity_death");
			if (entityDeathEvents != null) {
				for (JsonElement entityDeathEvent : entityDeathEvents) {
					mEvents.add(ZoneEntityDeathEvent.fromJson(entityDeathEvent));
				}
			}
		}

		//////////////////////////////////////// Fail if other keys exist ////////////////////////////////////////
		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();
			boolean isKnownKey = key.equals("namespace")
			|| key.equals("name")
			|| key.equals("display_name")
			|| key.equals("on_enter")
			|| key.equals("on_exit")
			|| key.equals("deaths")
			|| key.equals("logins")
			|| key.equals("events");

			if (!isKnownKey) {
				throw new Exception("Unknown zone property key: " + key);
			}
		}
	}

	public void addFromOther(Plugin plugin, ZoneProperty other) {
		if (!mNamespaceName.equals(other.mNamespaceName)
		    || !mName.equals(other.mName)) {
			MMLog.severe("Attempted to add two ZoneProperty objects of different properties!");
			return;
		}
		for (QuestComponent component : other.getEnterComponents()) {
			mEnterComponents.add(component);
		}
		for (QuestComponent component : other.getExitComponents()) {
			mExitComponents.add(component);
		}
		mDeaths.addAll(other.mDeaths);
		mLogins.addAll(other.mLogins);
	}

	public String getNamespaceName() {
		return mNamespaceName;
	}

	public String getName() {
		return mName;
	}
	
	public List<QuestDeath> getDeaths() {
		return mDeaths;
	}

	public List<QuestLogin> getLogins() {
		return mLogins;
	}

	public List<QuestComponent> getEnterComponents() {
		return mEnterComponents.getComponents();
	}

	public List<QuestComponent> getExitComponents() {
		return mExitComponents.getComponents();
	}
	
	// TODO: refactor these 2 to be the same method, accepting an enum EventType (ENTER, EXIT).
	// Needs to go over every call across all repos and rename accordingly.
	public void changeEvent(Plugin plugin, Player player) {
		mEnterComponents.run(new QuestContext(plugin, player, null));
	}

	public void exitEvent(Plugin plugin, Player player) {
		mExitComponents.run(new QuestContext(plugin, player, null));
	}
	// TODO: END

	@SuppressWarnings("unchecked")
	public <T> Collection<? extends T> getEvents(Class<T> eventClass) {
		return (Collection<? extends T>) mEvents.stream().filter(eventClass::isInstance).collect(Collectors.toList());
	}
}
