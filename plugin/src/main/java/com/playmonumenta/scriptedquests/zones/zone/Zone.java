package com.playmonumenta.scriptedquests.zones.zone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.bukkit.Axis;
import org.bukkit.util.Vector;

import com.playmonumenta.scriptedquests.zones.ZoneLayer;

/*
 * A fragment of a zone; this is used to find zones quickly, but not hold their properties.
 * Instead, each fragment points to its parent, a zone with properties.
 * Each zone also keeps track of its fragments.
 */
public class Zone extends BaseZone {
	private ZoneLayer mLayer;
	private String mName;
	private ArrayList<ZoneFragment> mFragments = new ArrayList<ZoneFragment>();
	private LinkedHashSet<String> mProperties = new LinkedHashSet<String>();

	/*
	 * Why yes, this does look weird. super() can only be called as the first method, and
	 * the json needs to be parsed first - but we don't want to duplicate code from the
	 * base class. By making this a static method, we made it possible to parse the
	 * json before handling anything else, including super(). This looks doubly weird
	 * for using the same method name - Zone - as the constructor, but this is a
	 * constructor like any other.
	 */
	public static Zone ConstructFromJson(ZoneLayer layer, JsonObject object, HashMap<String, ArrayList<String>> propertyGroups) throws Exception {
		if (layer == null) {
			throw new Exception("layer may not be null.");
		}
		if (object == null) {
			throw new Exception("object may not be null.");
		}
		if (propertyGroups == null) {
			throw new Exception("propertyGroups may not be null (but may be empty).");
		}

		Double[] corners = new Double[6];
		String name;
		LinkedHashSet<String> properties = new LinkedHashSet<String>();

		// Load the zone name
		if (object.get("name") == null ||
		    object.get("name").getAsString() == null ||
		    object.get("name").getAsString().isEmpty()) {
			throw new Exception("Failed to parse 'name'");
		}
		name = object.get("name").getAsString();

		// Load the zone location
		if (object.get("location") == null ||
		    object.get("location").getAsJsonObject() == null) {
			throw new Exception("Failed to parse 'location'");
		}
		JsonObject locationJson = object.get("location").getAsJsonObject();
		Set<Entry<String, JsonElement>> entries = locationJson.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();
			JsonElement value = ent.getValue();
			switch (key) {
			case "x1":
				corners[0] = value.getAsDouble();
				break;
			case "y1":
				corners[1] = value.getAsDouble();
				break;
			case "z1":
				corners[2] = value.getAsDouble();
				break;
			case "x2":
				corners[3] = value.getAsDouble();
				break;
			case "y2":
				corners[4] = value.getAsDouble();
				break;
			case "z2":
				corners[5] = value.getAsDouble();
				break;
			default:
				throw new Exception("Unknown location key: '" + key + "'");
			}
		}
		for (Double cornerAxis : corners) {
			if (cornerAxis == null) {
				throw new Exception("Location prereq must have x1 x2 y1 y2 z1 and z2");
			}
		}
		Vector pos1 = new Vector(corners[0], corners[1], corners[2]);
		Vector pos2 = new Vector(corners[3], corners[4], corners[5]);

		// Load the zone properties
		if (object.get("properties") == null ||
		    object.get("properties").getAsJsonArray() == null) {
			throw new Exception("Failed to parse 'properties'");
		}
		Iterator<JsonElement> iter = object.get("properties").getAsJsonArray().iterator();
		while (iter.hasNext()) {
			JsonElement element = iter.next();
			String propertyName = element.getAsString();
			applyProperty(propertyGroups, properties, propertyName);
		}

		return new Zone(layer, pos1, pos2, name, properties);
	}

	/*
	 * pos1 and pos2 are used similar to /fill:
	 * - Both are inclusive coordinates.
	 * - The minimum/maximum are determined for you.
	 */
	private Zone(ZoneLayer layer, Vector pos1, Vector pos2, String name, LinkedHashSet<String> properties) {
		super(pos1, pos2);
		mLayer = layer;
		mName = name;
		mProperties.addAll(properties);

		ZoneFragment initialFragment = new ZoneFragment(this);
		mFragments.add(initialFragment);
	}

	public void invalidate() {
		// Not strictly required, but speeds up garbage collection by eliminating loops.
		mFragments.clear();
	}

	/*
	 * Split all fragments of this zone by an overlapping zone, removing overlap.
	 */
	public void splitByOverlap(BaseZone overlap) {
		splitByOverlap(overlap, null);
	}

	/*
	 * Split all fragments of this zone by an overlapping zone,
	 * marking otherZone as the parent of the exact overlap fragment if
	 * it exists. Otherwise, the exact overlap fragment is discarded.
	 */
	public void splitByOverlap(BaseZone overlap, Zone otherZone) {
		ArrayList<ZoneFragment> newFragments = new ArrayList<ZoneFragment>();
		for (ZoneFragment fragment : mFragments) {
			BaseZone subOverlap = fragment.overlappingZone(overlap);

			if (subOverlap == null)
			{
				newFragments.add(fragment);
				continue;
			}

			newFragments.addAll(fragment.splitByOverlap(subOverlap, otherZone));
			fragment.invalidate();
		}
		// TODO if (newFragments.size() == 0) Print warning/error/something (total eclipse of this zone)
		mFragments = newFragments;
	}

	/*
	 * Minimize the number of uneclipsed fragments.
	 *
	 * This works with only one zone's fragments at a time, and doesn't
	 * need to be run again. This reduces n significantly for runtime.
	 *
	 * TODO Implement this based on the same function in:
	 * https://github.com/NickNackGus/monumenta-zone-prototype/blob/master/python/lib/zone/zone.py
	 * Also needs to invalidate fragments of merged zones.
	 */
	/*
	public void defragment() {
		if (mFragments.size() < 2) {
			return;
		}

		HashMap<int, HashMap<LinkedHashSet<int>, ZoneFragment>> allMergedCombinations = new HashMap<int, HashMap<LinkedHashSet<int>, ZoneFragment>>;
		private ArrayList<ZoneFragment> recursiveOptimalDefrag(HashMap<int, HashMap<LinkedHashSet<int>, ZoneFragment>> allMergedCombinations,
		                                                       ArrayList<ZoneFragment> resultsSoFar,
		                                                       LinkedHashSet<int> remainingIds);

		return;
	}
	*/

	public ZoneLayer getLayer() {
		return mLayer;
	}

	public String getLayerName() {
		return mLayer.getName();
	}

	public String getName() {
		return mName;
	}

	public ArrayList<ZoneFragment> getZoneFragments() {
		ArrayList<ZoneFragment> result = new ArrayList<ZoneFragment>();
		result.addAll(mFragments);
		return result;
	}

	public LinkedHashSet<String> getProperties() {
		LinkedHashSet<String> result = new LinkedHashSet<String>();
		result.addAll(mProperties);
		return result;
	}

	private static void applyProperty(HashMap<String, ArrayList<String>> propertyGroups, LinkedHashSet<String> currentProperties, String propertyName) throws Exception {
		applyProperty(propertyGroups, currentProperties, propertyName, false);
	}

	private static void applyProperty(HashMap<String, ArrayList<String>> propertyGroups, LinkedHashSet<String> currentProperties, String propertyName, boolean remove) throws Exception {
		if (propertyName == null) {
			throw new Exception("propertyName may not be null.");
		}
		if (currentProperties == null) {
			throw new Exception("currentProperties may not be null.");
		}
		if (propertyGroups == null) {
			throw new Exception("propertyGroups may not be null (but may be empty).");
		}

		char prefix = propertyName.charAt(0);
		if (prefix == '#') {
			ArrayList<String> propertyGroup = propertyGroups.get(propertyName.substring(1));
			if (propertyGroup == null) {
				throw new Exception("No such property group: " + propertyName);
			}

			for (String subPropertyName : propertyGroup) {
				applyProperty(propertyGroups, currentProperties, subPropertyName, remove);
			}
		} else if (prefix == '!') {
			applyProperty(propertyGroups, currentProperties, propertyName.substring(1), true);
		} else if (remove) {
			currentProperties.remove(propertyName);
		} else {
			currentProperties.add(propertyName);
		}
	}

	@Override
	public String toString() {
		return ("Zone(layer('" + getLayerName() + "'), "
		        + minCorner().toString() + ", "
		        + maxCorner().toString() + ", "
		        + mName + ", "
		        + mProperties.toString() + ")");
	}
}
