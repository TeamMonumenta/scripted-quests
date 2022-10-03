package com.playmonumenta.scriptedquests.zones;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public class ZonePropertyGroup {
	private final String mLayerName;
	private final String mName;
	private final List<String> mPropertyList = new ArrayList<>();

	public ZonePropertyGroup(JsonObject object) throws Exception {
		if (object == null) {
			throw new Exception("object may not be null.");
		}

		// Load the layer name
		@Nullable JsonElement layerNameElement = object.get("layer");
		if (layerNameElement == null) {
			throw new Exception("Failed to parse 'layer'");
		}
		@Nullable String layerName = layerNameElement.getAsString();
		if (layerName == null ||
			layerName.isEmpty()) {
			throw new Exception("Failed to parse 'layer'");
		}
		mLayerName = layerName;

		// Load the property group name
		@Nullable JsonElement nameElement = object.get("name");
		if (nameElement == null) {
			throw new Exception("Failed to parse 'name'");
		}
		@Nullable String name = nameElement.getAsString();
		if (name == null ||
		    name.isEmpty()) {
			throw new Exception("Failed to parse 'name'");
		}
		mName = name;

		// Load the properties
		@Nullable JsonElement propertiesElement = object.get("properties");
		if (propertiesElement == null) {
			throw new Exception("Failed to parse 'properties'");
		}
		@Nullable JsonArray propertiesJsonArray = propertiesElement.getAsJsonArray();
		if (propertiesJsonArray == null) {
			throw new Exception("Failed to parse 'properties");
		}
		int propertyGroupIndex = 0;
		for (JsonElement propertyNameElement : propertiesJsonArray) {
			String propertyName = propertyNameElement.getAsString();
			if (propertyName == null) {
				throw new Exception("Failed to parse 'properties[[" + propertyGroupIndex + "]'");
			}
			mPropertyList.add(propertyName);
		}
	}

	public String getLayerName() {
		return mLayerName;
	}

	public String getGroupName() {
		return mName;
	}

	public int getPropertyListSize() {
		return mPropertyList.size();
	}

	public List<String> getPropertyList() {
		return Collections.unmodifiableList(mPropertyList);
	}
}
