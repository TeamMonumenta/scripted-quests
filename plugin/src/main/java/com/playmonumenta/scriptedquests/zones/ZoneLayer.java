package com.playmonumenta.scriptedquests.zones;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.bukkit.util.Vector;

import com.playmonumenta.scriptedquests.zones.zone.BaseZone;
import com.playmonumenta.scriptedquests.zones.zone.Zone;
import com.playmonumenta.scriptedquests.zones.zone.ZoneFragment;
import com.playmonumenta.scriptedquests.zones.zonetree.BaseZoneTree;

public class ZoneLayer {
	private String mName;
	private ArrayList<Zone> mZones = new ArrayList<Zone>();
	private BaseZoneTree mZoneTree;

	public ZoneLayer(JsonObject object) throws Exception {
		if (object == null) {
			throw new Exception("object may not be null.");
		}

		// Load the layer name
		if (object.get("name") == null ||
		    object.get("name").getAsString() == null ||
		    object.get("name").getAsString().isEmpty()) {
			throw new Exception("Failed to parse 'name'");
		}
		mName = object.get("name").getAsString();


		// Load the property groups - why yes, this section is rather long.
		if (object.get("property_groups") == null ||
		    object.get("property_groups").getAsJsonObject() == null) {
			throw new Exception("Failed to parse 'property_groups'");
		}

		HashMap<String, ArrayList<String>> propertyGroups = new HashMap<String, ArrayList<String>>();
		HashMap<String, LinkedHashSet<String>> groupReferences = new HashMap<String, LinkedHashSet<String>>();

		JsonObject propertyGroupsJson = object.get("property_groups").getAsJsonObject();
		Set<Entry<String, JsonElement>> entries = propertyGroupsJson.entrySet();

		for (Entry<String, JsonElement> ent : entries) {
			String propertyGroupName = ent.getKey();
			JsonElement propertyGroupJson = ent.getValue();

			if (propertyGroupName == null || propertyGroupName.isEmpty()) {
				throw new Exception("Failed to parse 'property_groups': group name may not be empty.");
			}
			if (propertyGroupJson.getAsJsonArray() == null) {
				throw new Exception("Failed to parse 'property_groups." + propertyGroupName + "'");
			}

			ArrayList<String> propertyGroup = new ArrayList<String>();
			LinkedHashSet<String> ownGroupReferences = new LinkedHashSet<String>();

			Integer propertyGroupIndex = 0;
			Iterator<JsonElement> propertyGroupIter = propertyGroupJson.getAsJsonArray().iterator();

			while (propertyGroupIter.hasNext()) {
				JsonElement propertyNameElement = propertyGroupIter.next();
				String propertyName = propertyNameElement.getAsString();

				if (propertyName == null) {
					throw new Exception("Failed to parse 'property_groups." + propertyGroupName + "[" + Integer.toString(propertyGroupIndex) + "]'");
				}

				propertyGroup.add(propertyName);

				String groupReference = propertyName;
				if (groupReference.charAt(0) == '!') {
					groupReference = groupReference.substring(1);
				}
				if (groupReference.charAt(0) == '#') {
					ownGroupReferences.add(groupReference.substring(1));
				}

				propertyGroupIndex++;
			}

			groupReferences.put(propertyGroupName, ownGroupReferences);
			if (hasPropertyGroupLoop(groupReferences, propertyGroupName)) {
				throw new Exception("Loop detected in property group '" + propertyGroupName + "'. Groups may not reference themselves directly or indirectly.");
			}

			propertyGroups.put(propertyGroupName, propertyGroup);
		}


		// Load the zones
		if (object.get("zones") == null ||
		    object.get("zones").getAsJsonArray() == null) {
			throw new Exception("Failed to parse 'zones'");
		}

		Integer zoneIndex = 0;
		Iterator<JsonElement> zonesIter = object.get("zones").getAsJsonArray().iterator();

		while (zonesIter.hasNext()) {
			JsonElement zoneElement = zonesIter.next();
			if (zoneElement.getAsJsonObject() == null) {
				throw new Exception("Failed to parse 'zones[" + Integer.toString(zoneIndex) + "]'");
			}
			mZones.add(Zone.ConstructFromJson(this, zoneElement.getAsJsonObject(), propertyGroups));
			zoneIndex++;
		}


		// Split the zones into non-overlapping fragments
		removeOverlaps(mZones);

		// TODO Defragment to reduce fragment count (approx 2-3x on average)

		// Create list of all zone fragments
		ArrayList<ZoneFragment> zoneFragments = new ArrayList<ZoneFragment>();
		for (Zone zone : mZones) {
			zoneFragments.addAll(zone.getZoneFragments());
		}

		mZoneTree = BaseZoneTree.CreateZoneTree(zoneFragments);
	}

	public void invalidate() {
		// Not strictly required, but improves garbage collection by removing loops
		for (Zone zone : mZones) {
			zone.invalidate();
		}
	}

	public String getName() {
		return mName;
	}

	public ArrayList<Zone> getZones() {
		ArrayList<Zone> result = new ArrayList<Zone>();
		result.addAll(mZones);
		return result;
	}

	/*
	 * For a given location, return the fragment that contains it.
	 * Returns null if no fragment overlaps it.
	 */
	public ZoneFragment getZoneFragment(Vector loc) {
		return mZoneTree.getZoneFragment(loc);
	}

	/*
	 * For a given location, return the zone that contains it.
	 * Returns null if no zone overlaps it.
	 */
	public Zone getZone(Vector loc) {
		return mZoneTree.getZone(mName, loc);
	}

	private void removeOverlaps(ArrayList<Zone> zones) throws Exception {
		for (int i = 0; i < zones.size(); i++) {
			Zone outer = zones.get(i);
			for (Zone inner : zones.subList(i + 1, zones.size())) {
				BaseZone overlap = outer.overlappingZone(inner);
				if (overlap == null) {
					continue;
				}
				inner.splitByOverlap(overlap);
			}
		}
	}

	private boolean hasPropertyGroupLoop(HashMap<String, LinkedHashSet<String>> groupReferences, String startGroup) {
		return hasPropertyGroupLoop(groupReferences, startGroup, startGroup);
	}

	private boolean hasPropertyGroupLoop(HashMap<String, LinkedHashSet<String>> groupReferences, String startGroup, String continueGroup) {
		LinkedHashSet<String> subGroupReferences = groupReferences.get(continueGroup);
		if (subGroupReferences == null) {
			return false;
		}

		for (String subGroupReference : subGroupReferences) {
			if (subGroupReference.equals(startGroup)) {
				return true;
			}

			if (hasPropertyGroupLoop(groupReferences, startGroup, subGroupReference)) {
				return true;
			}
		}

		return false;
	}
}
