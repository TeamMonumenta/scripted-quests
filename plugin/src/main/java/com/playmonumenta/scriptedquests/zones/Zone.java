package com.playmonumenta.scriptedquests.zones;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

/*
 * A zone, to be split into fragments. This class holds the name and properties, and the fragments determine
 * if a point is inside the zone after overlaps are taken into account.
 */
public class Zone extends ZoneBase {
	private final ZoneLayer mLayer;
	private final String mName;
	private List<ZoneFragment> mFragments = new ArrayList<ZoneFragment>();
	private final Set<String> mProperties = new LinkedHashSet<String>();

	public static Zone constructFromJson(ZoneLayer layer, JsonObject object, Map<String, List<String>> propertyGroups) throws Exception {
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
		@Nullable String name;
		Set<String> properties = new LinkedHashSet<String>();

		// Load the zone name
		@Nullable JsonElement nameElement = object.get("name");
		if (nameElement == null) {
			throw new Exception("Failed to parse 'name'");
		}
		name = nameElement.getAsString();
		if (name == null ||
		    name.isEmpty()) {
			throw new Exception("Failed to parse 'name'");
		}

		// Load the zone location
		@Nullable JsonElement locationElement = object.get("location");
		if (locationElement == null) {
			throw new Exception("Failed to parse 'location'");
		}
		@Nullable JsonObject locationJson = locationElement.getAsJsonObject();
		if (locationJson == null) {
			throw new Exception("Failed to parse 'location'");
		}
		for (Map.Entry<String, JsonElement> ent : locationJson.entrySet()) {
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
		@Nullable JsonElement propertiesElement = object.get("properties");
		if (propertiesElement == null) {
			throw new Exception("Failed to parse 'properties'");
		}
		@Nullable JsonArray propertiesArray = propertiesElement.getAsJsonArray();
		if (propertiesArray == null) {
			throw new Exception("Failed to parse 'properties'");
		}
		Iterator<JsonElement> iter = propertiesArray.iterator();
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
	public Zone(ZoneLayer layer, Vector pos1, Vector pos2, String name, Set<String> properties) {
		super(pos1, pos2);
		mLayer = layer;
		mName = name;
		mProperties.addAll(properties);
	}

	/*
	 * Reset the fragments of this Zone so they can be recalculated without reloading this zone.
	 * Used to handle ZoneLayers from other plugins. This should only be called by its ZoneLayer.
	 */
	protected void reloadFragments() {
		mFragments.clear();
		mFragments.add(new ZoneFragment(this));
	}

	/*
	 * Remove references to fragments from this zone.
	 *
	 * Note that the fragments point to the zone, too. This only prevents further
	 * modification of the old fragments from the current zone object.
	 *
	 * Not strictly required, but speeds up garbage collection by eliminating loops.
	 */
	protected void invalidate() {
		mFragments.clear();
	}

	/*
	 * Split all fragments of this zone by an overlapping zone, removing overlap.
	 */
	protected boolean splitByOverlap(ZoneBase overlap, Zone otherZone) {
		return splitByOverlap(overlap, otherZone, false);
	}

	/*
	 * Split all fragments of this zone by an overlapping zone,
	 * marking otherZone as the parent of the exact overlap fragment if
	 * it exists. Otherwise, the exact overlap fragment is discarded.
	 *
	 * Returns true if the zone being overlapped has been completely
	 * eclipsed by the other zone.
	 */
	protected boolean splitByOverlap(ZoneBase overlap, Zone otherZone, boolean includeOther) {
		List<ZoneFragment> newFragments = new ArrayList<ZoneFragment>();
		for (ZoneFragment fragment : mFragments) {
			@Nullable ZoneBase subOverlap = fragment.overlappingZone(overlap);

			if (subOverlap == null) {
				newFragments.add(fragment);
				continue;
			}

			newFragments.addAll(fragment.splitByOverlap(subOverlap, otherZone, includeOther));
			fragment.invalidate();
		}
		mFragments = newFragments;
		return newFragments.size() == 0;
	}

	/*
	 * Minimize the number of uneclipsed fragments.
	 *
	 * This works with only one zone's fragments at a time, and doesn't
	 * need to be run again. This reduces n significantly for runtime.
	 */
	protected void defragment() {
		if (mFragments.size() < 2) {
			return;
		}

		// Load current fragments into defragmenter
		ZoneDefragmenter defragmenter = new ZoneDefragmenter(mFragments);

		// Invalidate all current fragments.
		invalidate();

		// Get fewest fragments that represent the same thing (mostly large combos)
		mFragments = defragmenter.optimalMerge();
	}

	public ZoneLayer getLayer() {
		return mLayer;
	}

	public String getLayerName() {
		return mLayer.getName();
	}

	public String getName() {
		return mName;
	}

	public List<ZoneFragment> getZoneFragments() {
		return new ArrayList<ZoneFragment>(mFragments);
	}

	public Set<String> getProperties() {
		return new LinkedHashSet<String>(mProperties);
	}

	public boolean hasProperty(String propertyName) {
		return mProperties.contains(propertyName);
	}

	private static void applyProperty(Map<String, List<String>> propertyGroups, Set<String> currentProperties, String propertyName) throws Exception {
		applyProperty(propertyGroups, currentProperties, propertyName, false);
	}

	private static void applyProperty(Map<String, List<String>> propertyGroups, Set<String> currentProperties, String propertyName, boolean remove) throws Exception {
		if (propertyName == null) {
			throw new Exception("propertyName may not be null.");
		}
		if (propertyName.isEmpty()) {
			throw new Exception("propertyName may not be empty (including after the prefix # or !).");
		}
		if (currentProperties == null) {
			throw new Exception("currentProperties may not be null.");
		}
		if (propertyGroups == null) {
			throw new Exception("propertyGroups may not be null (but may be empty).");
		}

		char prefix = propertyName.charAt(0);
		if (prefix == '#') {
			@Nullable List<String> propertyGroup = propertyGroups.get(propertyName.substring(1));
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

	public boolean equals(Zone other) {
		return (super.equals(other) &&
		        getLayerName().equals(other.getLayerName()) &&
		        getName().equals(other.getName()));
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(Object o) {
		if (!(o instanceof Zone)) {
			return false;
		}

		Zone other = (Zone)o;
		return (super.equals(other) &&
		        getLayerName().equals(other.getLayerName()) &&
		        getName().equals(other.getName()));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31*result + getLayerName().hashCode();
		result = 31*result + getName().hashCode();
		return result;
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
