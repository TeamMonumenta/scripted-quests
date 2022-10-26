package com.playmonumenta.scriptedquests.zones;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class ZonePropertyGroupManager {
	private final Map<String, Map<String, ZonePropertyGroup>> mZonePropertyGroups = new HashMap<>();

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, Collection<CommandSender> senders) {
		mZonePropertyGroups.clear();

		QuestUtils.loadScriptedQuests(plugin, "zone_property_groups", senders, (object) -> {
			// Load this file into a ZonePropertyGroup object
			ZonePropertyGroup propertyGroup = new ZonePropertyGroup(object);
			String layerName = propertyGroup.getLayerName();
			String name = propertyGroup.getGroupName();

			Map<String, ZonePropertyGroup> layerGroups = mZonePropertyGroups.computeIfAbsent(layerName, key -> new HashMap<>());
			if (layerGroups.containsKey(name)) {
				throw new Exception("ZonePropertyGroup in layer '" + layerName + "' named '" + name + "' already exists!");
			}
			layerGroups.put(name, propertyGroup);

			return layerName + ":" + name + ":" + propertyGroup.getPropertyListSize();
		});

		for (Map.Entry<String, Map<String, ZonePropertyGroup>> layerEntry : mZonePropertyGroups.entrySet()) {
			String layerName = layerEntry.getKey();
			Map<String, ZonePropertyGroup> layerGroups = layerEntry.getValue();
			Map<String, Set<String>> groupReferences = new HashMap<>();

			// Gather group references
			for (Map.Entry<String, ZonePropertyGroup> propertyGroupEntry : layerGroups.entrySet()) {
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
				Component error = Component.text("ZonePropertyGroup loop(s) detected and removed in layer '"
					+ layerName + "': " + selfContainingGroups, NamedTextColor.RED);
				for (CommandSender sender : senders) {
					sender.sendMessage(error);
				}
			}
			for (String selfContainingGroup : selfContainingGroups) {
				layerGroups.remove(selfContainingGroup);
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
		for (String nextGroup : groupReferences.get(currentGroup)) {
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

	public Set<String> resolveProperties(String layerName, List<String> originalProperties) {
		Set<String> result = new TreeSet<>();
		@Nullable Map<String, ZonePropertyGroup> layerGroups = mZonePropertyGroups.get(layerName);
		if (layerGroups != null) {
			resolveProperties(layerGroups, result, originalProperties, false);
		}
		return result;
	}

	private void resolveProperties(Map<String, ZonePropertyGroup> layerGroups,
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
				@Nullable ZonePropertyGroup childGroup = layerGroups.get(childGroupName);
				if (childGroup == null) {
					continue;
				}
				resolveProperties(layerGroups, loadedProperties, childGroup.getPropertyList(), locallyRemoving);
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
