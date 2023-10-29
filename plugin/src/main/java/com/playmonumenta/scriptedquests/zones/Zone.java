package com.playmonumenta.scriptedquests.zones;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.util.Vector;

/*
 * A zone, to be split into fragments. This class holds the name and properties, and the fragments determine
 * if a point is inside the zone after overlaps are taken into account.
 */
public class Zone extends ZoneBase {
	private final ZoneNamespace mNamespace;
	private final String mName;
	private List<ZoneFragment> mFragments = new ArrayList<>();
	private final Set<String> mProperties = new LinkedHashSet<>();

	public static Zone constructFromJson(ZoneNamespace namespace, JsonObject object) throws Exception {
		if (namespace == null) {
			throw new Exception("namespace may not be null.");
		}
		if (object == null) {
			throw new Exception("object may not be null.");
		}

		Double[] corners = new Double[6];
		@Nullable String name;

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
				case "x1" -> corners[0] = value.getAsDouble();
				case "y1" -> corners[1] = value.getAsDouble();
				case "z1" -> corners[2] = value.getAsDouble();
				case "x2" -> corners[3] = value.getAsDouble();
				case "y2" -> corners[4] = value.getAsDouble();
				case "z2" -> corners[5] = value.getAsDouble();
				default -> throw new Exception("Unknown location key: '" + key + "'");
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
		List<String> rawProperties = new ArrayList<>();
		for (JsonElement element : propertiesArray) {
			String propertyName = element.getAsString();
			if (propertyName == null || propertyName.isBlank()) {
				throw new Exception("Property may not be empty");
			}
			rawProperties.add(propertyName);
		}
		Set<String> properties = Plugin.getInstance().mZonePropertyGroupManager.resolveProperties(namespace.getName(), rawProperties);

		return new Zone(namespace, pos1, pos2, name, properties);
	}

	/*
	 * pos1 and pos2 are used similar to /fill:
	 * - Both are inclusive coordinates.
	 * - The minimum/maximum are determined for you.
	 */
	public Zone(ZoneNamespace namespace, Vector pos1, Vector pos2, String name, Set<String> properties) {
		super(pos1, pos2);
		mNamespace = namespace;
		mName = name;
		mProperties.addAll(properties);
	}

	/*
	 * Reset the fragments of this Zone so they can be recalculated without reloading this zone.
	 * Used to handle ZoneNamespaces from other plugins. This should only be called by its ZoneNamespace.
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
		List<ZoneFragment> newFragments = new ArrayList<>();
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

	public ZoneNamespace getNamespace() {
		return mNamespace;
	}

	public String getNamespaceName() {
		return mNamespace.getName();
	}

	public String getName() {
		return mName;
	}

	public List<ZoneFragment> getZoneFragments() {
		return new ArrayList<>(mFragments);
	}

	public Set<String> getProperties() {
		return Collections.unmodifiableSet(mProperties);
	}

	public boolean hasProperty(String propertyName) {
		boolean negate = propertyName.charAt(0) == '!';
		if (negate) {
			propertyName = propertyName.substring(1);
		}
		return negate ^ mProperties.contains(propertyName);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Zone other) {
			return (super.equals(other) &&
				getNamespaceName().equals(other.getNamespaceName()) &&
				getName().equals(other.getName()));
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31*result + getNamespaceName().hashCode();
		result = 31*result + getName().hashCode();
		return result;
	}

	@Override
	public String toString() {
		return ("Zone(namespace('" + getNamespaceName() + "'), "
		        + minCorner().toString() + ", "
		        + maxCorner().toString() + ", "
		        + mName + ", "
		        + mProperties.toString() + ")");
	}
}
