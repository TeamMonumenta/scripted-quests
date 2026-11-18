package com.playmonumenta.scriptedquests.quests;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import com.playmonumenta.scriptedquests.quests.components.QuestComponentList;
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
 * Everything one zone property does, in one file: on_enter, on_exit, on_death, on_login
 * and events. Every section is optional, so a file that only carries on_death rules is
 * valid and does nothing when the property is entered or left.
 *
 * Properties are grouped into namespaces by the zone geometry, which is loaded from
 * zone_namespaces. A property name may start with # in that geometry to refer to a group
 * of properties; the behavior file itself is always addressed by its plain name.
 */
public class ZoneProperty {
	private final String mNamespaceName;
	private final String mName;
	private final String mDisplayName;
	private final QuestComponentList mEnterComponents = new QuestComponentList();
	private final QuestComponentList mExitComponents = new QuestComponentList();
	private final List<QuestDeath> mDeaths = new ArrayList<>();
	private final List<QuestLogin> mLogins = new ArrayList<>();
	private final List<ZoneEvent> mEvents = new ArrayList<>();

	public ZoneProperty(JsonObject object) throws Exception {
		//////////////////////////////////////// namespace (Required) ////////////////////////////////////////
		JsonElement namespaceElement = object.get("namespace");
		if (namespaceElement == null) {
			throw new Exception("'namespace' entry is required");
		}
		mNamespaceName = namespaceElement.getAsString();
		if (mNamespaceName == null || mNamespaceName.isEmpty()) {
			throw new Exception("Failed to parse 'namespace' as string");
		}

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

		//////////////////////////////////////// on_enter / on_exit (Optional) ////////////////////////////////////////
		for (JsonElement entry : getJsonArrayOrEmpty(object, "on_enter")) {
			// TODO: Refactor so that components only require a linkage to the top-level item, not a name/entity type
			mEnterComponents.add(new QuestComponent(mName, mDisplayName, EntityType.PLAYER, entry));
		}
		for (JsonElement entry : getJsonArrayOrEmpty(object, "on_exit")) {
			mExitComponents.add(new QuestComponent(mName, mDisplayName, EntityType.PLAYER, entry));
		}

		//////////////////////////////////////// on_death / on_login (Optional) ////////////////////////////////////////
		for (JsonElement entry : getJsonArrayOrEmpty(object, "on_death")) {
			JsonObject entryObject = entry.getAsJsonObject();
			mDeaths.add(new QuestDeath(entryObject, priorityOf(entryObject, "on_death")));
		}
		for (JsonElement entry : getJsonArrayOrEmpty(object, "on_login")) {
			JsonObject entryObject = entry.getAsJsonObject();
			mLogins.add(new QuestLogin(entryObject, priorityOf(entryObject, "on_login")));
		}

		//////////////////////////////////////// events (optional) ////////////////////////////////////////
		JsonObject eventsObject = object.getAsJsonObject("events");
		if (eventsObject != null) {
			addEvents(eventsObject);
		}

		//////////////////////////////////////// Fail if other keys exist ////////////////////////////////////////
		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("namespace") && !key.equals("name") && !key.equals("display_name")
				    && !key.equals("on_enter") && !key.equals("on_exit")
				    && !key.equals("on_death") && !key.equals("on_login") && !key.equals("events")) {
				throw new Exception("Unknown quest key: " + key);
			}
		}
	}

	private void addEvents(JsonObject eventsObject) throws Exception {
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

	private JsonArray getJsonArrayOrEmpty(JsonObject object, String key) throws Exception {
		JsonElement element = object.get(key);
		if (element == null) {
			return new JsonArray();
		}
		JsonArray array = element.getAsJsonArray();
		if (array == null) {
			throw new Exception("Failed to parse '" + key + "' as JSON array");
		}
		return array;
	}

	/*
	 * Rules without an explicit priority are evaluated last, so that a new rule cannot
	 * silently preempt one that already declares its precedence.
	 */
	private int priorityOf(JsonObject entry, String section) {
		JsonElement priority = entry.get("priority");
		if (priority == null) {
			MMLog.warning("Zone property '" + mNamespaceName + ":" + mName + "' has an " + section
				+ " entry without a priority, it will be evaluated last");
			return Integer.MAX_VALUE;
		}
		return priority.getAsInt();
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

	private List<QuestComponent> getEnterComponents() {
		return mEnterComponents.getComponents();
	}

	private List<QuestComponent> getExitComponents() {
		return mExitComponents.getComponents();
	}

	public int getComponentCount() {
		return mEnterComponents.getComponents().size() + mExitComponents.getComponents().size();
	}

	public void changeEnterEvent(Plugin plugin, Player player) {
		mEnterComponents.run(new QuestContext(plugin, player, null));
	}

	public void changeExitEvent(Plugin plugin, Player player) {
		mExitComponents.run(new QuestContext(plugin, player, null));
	}

	@SuppressWarnings("unchecked")
	public <T> Collection<? extends T> getEvents(Class<T> eventClass) {
		return (Collection<? extends T>) mEvents.stream().filter(eventClass::isInstance).collect(Collectors.toList());
	}
}
