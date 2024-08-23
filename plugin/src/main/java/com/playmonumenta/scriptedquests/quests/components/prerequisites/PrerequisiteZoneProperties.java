package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.zones.Zone;
import com.playmonumenta.scriptedquests.zones.ZoneFragment;
import com.playmonumenta.scriptedquests.zones.ZoneManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Location;

public class PrerequisiteZoneProperties implements PrerequisiteBase {
	private final Set<String> mMentionedNamespaces;
	private final Set<String> mRequiredNamespaces;
	private final Map<String, Set<String>> mProperties;
	private final Map<String, Set<String>> mNotProperties;

	public PrerequisiteZoneProperties(JsonElement element) throws Exception {
		Set<String> requiredNamespaces = new HashSet<>();
		Map<String, Set<String>> properties = new HashMap<>();
		Map<String, Set<String>> notProperties = new HashMap<>();

		JsonObject object = element.getAsJsonObject();
		for (Map.Entry<String, JsonElement> ent : object.entrySet()) {
			String namespaceName = ent.getKey();
			JsonArray jsonNamespaceProps = ent.getValue().getAsJsonArray();

			Set<String> namespaceProps = new HashSet<>();
			Set<String> namespaceNotProps = new HashSet<>();

			if (jsonNamespaceProps.size() == 0) {
				requiredNamespaces.add(namespaceName);
				continue;
			}

			for (JsonElement jsonProp : jsonNamespaceProps) {
				String prop = jsonProp.getAsString();
				if (prop.startsWith("!")) {
					namespaceNotProps.add(prop.substring(1));
				} else {
					namespaceProps.add(prop);
				}
			}

			if (!namespaceProps.isEmpty()) {
				properties.put(namespaceName, namespaceProps);
			}

			if (!namespaceNotProps.isEmpty()) {
				notProperties.put(namespaceName, namespaceNotProps);
			}
		}

		requiredNamespaces.addAll(properties.keySet());

		Set<String> mentionedNamespaces = new HashSet<>(requiredNamespaces);
		mentionedNamespaces.addAll(notProperties.keySet());

		mMentionedNamespaces = mentionedNamespaces;
		mRequiredNamespaces = requiredNamespaces;
		mProperties = properties;
		mNotProperties = notProperties;
	}

	@Override
	public boolean prerequisiteMet(QuestContext context) {
		Location loc = context.getEntityUsedForPrerequisites().getLocation();
		ZoneFragment fragment = ZoneManager.getInstance().getZoneFragment(loc);

		// If no fragment exists here, neither do zones nor namespaces.
		if (fragment == null) {
			return mRequiredNamespaces.isEmpty();
		}

		for (String namespaceName : mMentionedNamespaces) {
			Zone zone = fragment.getParent(loc.getWorld(), namespaceName);

			// If there is no zone in this namespace...
			if (zone == null) {
				// ...check that the namespace is not a prerequisite...
				if (mRequiredNamespaces.contains(namespaceName)) {
					return false;
				}
				// ...and continue if it is not.
				continue;
			}

			// The zone's properties.
			Set<String> properties = zone.getProperties();

			// If there are any property prerequisites, ensure all are found.
			Set<String> requiredProperties = mProperties.get(namespaceName);
			if (requiredProperties != null && !properties.containsAll(requiredProperties)) {
				return false;
			}

			// For any negated property prerequisites, ensure none are found.
			// disjoint returns true if no elements are in common.
			Set<String> requiredNotProperties = mNotProperties.get(namespaceName);
			if (requiredNotProperties != null &&
			    !Collections.disjoint(requiredNotProperties, properties)) {
				return false;
			}
		}
		return true;
	}
}
