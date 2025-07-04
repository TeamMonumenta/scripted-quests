package com.playmonumenta.scriptedquests.zones;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ZonePropertyGroupManager {
	private final Map<String, Map<String, ZonePropertyGroup>> mZonePropertyGroups = new HashMap<>();

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, Audience audience) {
		mZonePropertyGroups.clear();

		QuestUtils.loadScriptedQuests(plugin, "zone_property_groups", audience, (object) -> {
			// Load this file into a ZonePropertyGroup object
			ZonePropertyGroup propertyGroup = new ZonePropertyGroup(object);
			String namespaceName = propertyGroup.getNamespaceName();
			String name = propertyGroup.getGroupName();

			Map<String, ZonePropertyGroup> namespacePropertyGroups = mZonePropertyGroups.computeIfAbsent(namespaceName, key -> new HashMap<>());
			if (namespacePropertyGroups.containsKey(name)) {
				throw new Exception("ZonePropertyGroup in namespace '" + namespaceName + "' named '" + name + "' already exists!");
			}
			namespacePropertyGroups.put(name, propertyGroup);

			return namespaceName + ":" + name + ":" + propertyGroup.getPropertyListSize();
		});

		for (Map.Entry<String, Map<String, ZonePropertyGroup>> namespaceEntry : mZonePropertyGroups.entrySet()) {
			String namespaceName = namespaceEntry.getKey();
			Map<String, ZonePropertyGroup> namespaceGroups = namespaceEntry.getValue();
			Map<String, Set<String>> groupReferences = new HashMap<>();

			// Gather group references
			for (Map.Entry<String, ZonePropertyGroup> propertyGroupEntry : namespaceGroups.entrySet()) {
				String groupName = propertyGroupEntry.getKey();
				ZonePropertyGroup group = propertyGroupEntry.getValue();

				Set<String> ownGroupReferences = new TreeSet<>();
				groupReferences.put(groupName, ownGroupReferences);

				for (String property : group.getPropertyList()) {
					if (property.charAt(0) == '!') {
						property = property.substring(1);
					}
					if (property.charAt(0) != '#') {
						continue;
					}
					ownGroupReferences.add(property.substring(1));
				}
			}

			// Detect loops (must be recursive)
			Set<String> selfContainingGroups = new TreeSet<>();
			for (String groupName : groupReferences.keySet()) {
				Set<String> seenByGroup = new TreeSet<>();
				detectLoops(groupReferences, selfContainingGroups, groupName, seenByGroup, groupName);
			}

			// Report and remove loops
			if (!selfContainingGroups.isEmpty()) {
				Component error = Component.text("ZonePropertyGroup loop(s) detected and removed in namespace '"
					+ namespaceName + "': " + selfContainingGroups, NamedTextColor.RED);
				audience.sendMessage(error);
			}
			for (String selfContainingGroup : selfContainingGroups) {
				namespaceGroups.remove(selfContainingGroup);
			}
		}
	}

	private void detectLoops(Map<String, Set<String>> groupReferences,
	                         Set<String> selfContainingGroups,
							 String startingGroup,
	                         Set<String> seenByGroup,
	                         String currentGroup) {
		// Loop detected, but it's not the starting group; we'll deal with it later
		// Note that selfContainingGroups is not checked as a shortcut,
		// since all groups in a loop should be reported
		if (seenByGroup.contains(currentGroup)) {
			return;
		}
		seenByGroup.add(currentGroup);

		// Check next groups
		Set<String> nextGroups = groupReferences.get(currentGroup);
		if (nextGroups == null) {
			throw new IllegalStateException("Expected to find group " + currentGroup + " in groupReferences but it was missing");
		}
		for (String nextGroup : nextGroups) {
			// If we found the starting group, it is a self containing group
			if (startingGroup.equals(nextGroup)) {
				selfContainingGroups.add(startingGroup);
				return;
			}

			detectLoops(groupReferences,
				selfContainingGroups,
				startingGroup,
				seenByGroup,
				nextGroup);
		}
	}

	public Set<String> resolveProperties(String namespaceName, List<String> originalProperties) {
		Set<String> result = new TreeSet<>();
		@Nullable Map<String, ZonePropertyGroup> namespaceGroups = mZonePropertyGroups.get(namespaceName);
		if (namespaceGroups == null) {
			namespaceGroups = new HashMap<>();
		}
		resolveProperties(namespaceGroups, result, originalProperties, false);
		return result;
	}

	private void resolveProperties(Map<String, ZonePropertyGroup> namespaceGroups,
	                               Set<String> loadedProperties,
	                               List<String> toProcess,
	                               boolean removingProperties) {
		for (String property : toProcess) {
			boolean locallyRemoving = property.charAt(0) == '!';
			if (locallyRemoving) {
				property = property.substring(1);
			}
			locallyRemoving = removingProperties || locallyRemoving;

			if (property.charAt(0) == '#') {
				String childGroupName = property.substring(1);
				@Nullable ZonePropertyGroup childGroup = namespaceGroups.get(childGroupName);
				if (childGroup == null) {
					continue;
				}
				resolveProperties(namespaceGroups, loadedProperties, childGroup.getPropertyList(), locallyRemoving);
				continue;
			}

			if (locallyRemoving) {
				loadedProperties.remove(property);
			} else {
				loadedProperties.add(property);
			}
		}
	}
}
