package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.zones.Zone;
import com.playmonumenta.scriptedquests.zones.ZoneFragment;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Location;

public class PrerequisiteZoneProperties implements PrerequisiteBase {
	private final Set<String> mMentionedLayers;
	private final Set<String> mRequiredLayers;
	private final Map<String, Set<String>> mProperties;
	private final Map<String, Set<String>> mNotProperties;

	public PrerequisiteZoneProperties(JsonElement element) throws Exception {
		Set<String> requiredLayers = new HashSet<String>();
		Map<String, Set<String>> properties = new HashMap<String, Set<String>>();
		Map<String, Set<String>> notProperties = new HashMap<String, Set<String>>();

		JsonObject object = element.getAsJsonObject();
		for (Map.Entry<String, JsonElement> ent : object.entrySet()) {
			String layerName = ent.getKey();
			JsonArray jsonLayerProps = ent.getValue().getAsJsonArray();

			Set<String> layerProps = new HashSet<String>();
			Set<String> layerNotProps = new HashSet<String>();

			if (jsonLayerProps.size() == 0) {
				requiredLayers.add(layerName);
				continue;
			}

			for (JsonElement jsonProp : jsonLayerProps) {
				String prop = jsonProp.getAsString();
				if (prop.startsWith("!")) {
					layerNotProps.add(prop.substring(1));
				} else {
					layerProps.add(prop);
				}
			}

			if (!layerProps.isEmpty()) {
				properties.put(layerName, layerProps);
			}

			if (!layerNotProps.isEmpty()) {
				notProperties.put(layerName, layerNotProps);
			}
		}

		requiredLayers.addAll(properties.keySet());

		Set<String> mentionedLayers = new HashSet<String>(requiredLayers);
		mentionedLayers.addAll(notProperties.keySet());

		mMentionedLayers = mentionedLayers;
		mRequiredLayers = requiredLayers;
		mProperties = properties;
		mNotProperties = notProperties;
	}

	@Override
	public boolean prerequisiteMet(QuestContext context) {
		Location loc = context.getEntityUsedForPrerequisites().getLocation();
		ZoneFragment fragment = Plugin.getInstance().mZoneManager.getZoneFragment(loc);

		// If no fragment exists here, neither do zones or layers.
		if (fragment == null) {
			return mRequiredLayers.isEmpty();
		}

		for (String layerName : mMentionedLayers) {
			Zone zone = fragment.getParent(layerName);

			// If there is no zone on this layer...
			if (zone == null) {
				// ...check that the layer is not a prerequisite...
				if (mRequiredLayers.contains(layerName)) {
					return false;
				}
				// ...and continue if it is not.
				continue;
			}

			// The zone's properties.
			Set<String> properties = zone.getProperties();

			// If there are any property prerequisites, ensure all are found.
			Set<String> requiredProperties = mProperties.get(layerName);
			if (requiredProperties != null && !properties.containsAll(requiredProperties)) {
				return false;
			}

			// For any negated property prerequisites, ensure none are found.
			// disjoint returns true if no elements are in common.
			Set<String> requiredNotProperties = mNotProperties.get(layerName);
			if (requiredNotProperties != null &&
			    !Collections.disjoint(requiredNotProperties, properties)) {
				return false;
			}
		}
		return true;
	}
}
