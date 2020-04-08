package com.playmonumenta.scriptedquests.zones.zone;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.util.Vector;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.zones.ZoneLayer;

/*
 * A zone, to be split into fragments. This class holds the name and properties, and the fragments determine
 * if a point is inside the zone after overlaps are taken into account.
 */
public class Zone<T> extends BaseZone {
	private final ZoneLayer<T> mLayer;
	private final String mName;
	private List<ZoneFragment<T>> mFragments = new ArrayList<ZoneFragment<T>>();
	private final Set<String> mProperties = new LinkedHashSet<String>();
	private final T mTag;

	public static <T> Zone<T> constructFromJson(ZoneLayer<T> layer, JsonObject object, Map<String, List<String>> propertyGroups, T tag) throws Exception {
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
		Set<String> properties = new LinkedHashSet<String>();

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

		return new Zone<T>(layer, pos1, pos2, name, properties, tag);
	}

	/*
	 * pos1 and pos2 are used similar to /fill:
	 * - Both are inclusive coordinates.
	 * - The minimum/maximum are determined for you.
	 */
	public Zone(ZoneLayer<T> layer, Vector pos1, Vector pos2, String name, Set<String> properties, T tag) {
		super(pos1, pos2);
		mLayer = layer;
		mName = name;
		mProperties.addAll(properties);
		mTag = tag;
	}

	/*
	 * Reset the fragments of this Zone so they can be recalculated without reloading this zone.
	 * Used to handle ZoneLayers from other plugins. This should only be called by its ZoneLayer.
	 */
	public void reloadFragments() {
		mFragments.clear();
		mFragments.add(new ZoneFragment<T>(this));
	}

	/*
	 * Remove references to fragments from this zone.
	 *
	 * Note that the fragments point to the zone, too. This only prevents further
	 * modification of the old fragments from the current zone object.
	 *
	 * Not strictly required, but speeds up garbage collection by eliminating loops.
	 */
	public void invalidate() {
		mFragments.clear();
	}

	public T getTag() {
		return mTag;
	}

	/*
	 * Split all fragments of this zone by an overlapping zone, removing overlap.
	 */
	public boolean splitByOverlap(BaseZone overlap, Zone<T> otherZone) {
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
	public boolean splitByOverlap(BaseZone overlap, Zone<T> otherZone, boolean includeOther) {
		List<ZoneFragment<T>> newFragments = new ArrayList<ZoneFragment<T>>();
		for (ZoneFragment<T> fragment : mFragments) {
			BaseZone subOverlap = fragment.overlappingZone(overlap);

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
	public void defragment() {
		if (mFragments.size() < 2) {
			return;
		}

		// Load current fragments into defragmenter
		ZoneDefragmenter<T> defragmenter = new ZoneDefragmenter<T>(mFragments);

		// Invalidate all current fragments.
		invalidate();

		// Get fewest fragments that represent the same thing (mostly large combos)
		mFragments = defragmenter.optimalMerge();
	}

	public ZoneLayer<T> getLayer() {
		return mLayer;
	}

	public String getLayerName() {
		return mLayer.getName();
	}

	public String getName() {
		return mName;
	}

	public List<ZoneFragment<T>> getZoneFragments() {
		return new ArrayList<ZoneFragment<T>>(mFragments);
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
			List<String> propertyGroup = propertyGroups.get(propertyName.substring(1));
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

	public boolean equals(Zone<T> other) {
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

		Zone<T> other = (Zone<T>)o;
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
