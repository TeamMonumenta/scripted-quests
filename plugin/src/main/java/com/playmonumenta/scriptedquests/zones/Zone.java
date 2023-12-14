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
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

/*
 * A zone, to be split into fragments. This class holds the name and properties, and the fragments determine
 * if a point is inside the zone after overlaps are taken into account.
 */
public class Zone extends ZoneBase {
	private final ZoneNamespace mNamespace;
	private final String mName;
	private final String mWorldRegex;
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
		if (name == null || name.isEmpty()) {
			throw new Exception("Failed to parse 'name'");
		}

		// This gets inserted from the ZoneNamespace file if missing from the zone json
		@Nullable JsonElement worldElement = object.get("world_name");
		if (worldElement == null) {
			throw new Exception("Failed to find inserted 'world_name'");
		}
		@Nullable String worldRegexStr = worldElement.getAsString();
		if (worldRegexStr == null || worldRegexStr.isEmpty()) {
			throw new Exception("Failed to parse 'world_name'");
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
		List<String> rawProperties = getProperties(propertiesElement);
		Set<String> properties = Plugin.getInstance().mZonePropertyGroupManager.resolveProperties(namespace.getName(), rawProperties);

		return new Zone(namespace, worldRegexStr, pos1, pos2, name, properties);
	}

	private static List<String> getProperties(@Nullable JsonElement propertiesElement) throws Exception {
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
		return rawProperties;
	}

	/*
	 * pos1 and pos2 are used similar to /fill:
	 * - Both are inclusive coordinates.
	 * - The minimum/maximum are determined for you.
	 */
	public Zone(ZoneNamespace namespace, String worldRegex, Vector pos1, Vector pos2, String name, Set<String> properties) {
		super(pos1, pos2);
		mNamespace = namespace;
		mWorldRegex = worldRegex;
		mName = name;
		mProperties.addAll(properties);
	}

	public ZoneNamespace getNamespace() {
		return mNamespace;
	}

	public String getNamespaceName() {
		return mNamespace.getName();
	}

	public String getWorldRegex() {
		return mWorldRegex;
	}

	public boolean matchesWorld(World world) {
		return ZoneManager.getInstance().getWorldRegexMatcher().matches(world, mWorldRegex);
	}

	public boolean within(Location location) {
		return matchesWorld(location.getWorld()) && within(location.toVector());
	}

	public String getName() {
		return mName;
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
