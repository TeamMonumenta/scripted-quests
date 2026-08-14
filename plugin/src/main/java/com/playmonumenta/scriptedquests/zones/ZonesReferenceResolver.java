package com.playmonumenta.scriptedquests.zones;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.common.zones.Zone;
import com.playmonumenta.common.zones.ZoneNamespace;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ZonesReferenceResolver {
	private static class ZoneNamespaceFile {
		protected final String mName;
		protected final @Nullable String mWorldRegex;
		protected final @Nullable String mReferenceId;
		protected boolean mHidden = false;
		protected final List<JsonObject> mZoneOrder = new ArrayList<>();
		protected final Set<String> mRequiredRefs = new HashSet<>();

		protected ZoneNamespaceFile(JsonObject object) throws Exception {
			if (object == null) {
				throw new Exception("object may not be null.");
			}

			// Load the namespace name
			@Nullable JsonElement nameElement = object.get("name");
			if (nameElement == null) {
				throw new Exception("Failed to parse 'name'");
			}
			@Nullable String name = nameElement.getAsString();
			if (name == null || name.isEmpty()) {
				throw new Exception("Failed to parse 'name'");
			}
			mName = name;

			// Load the reference ID. If set, this file will only be used when referenced by a main or referenced file
			@Nullable JsonElement referenceElement = object.get("reference");
			if (referenceElement == null) {
				mReferenceId = null;
			} else {
				@Nullable String reference = referenceElement.getAsString();
				if (reference == null || reference.isEmpty()) {
					throw new Exception("Failed to parse 'reference'");
				}
				mReferenceId = reference;
			}

			// Load the world name regex.
			// If absent, use ".*" for all worlds on main files, and inherit from parent on reference files.
			@Nullable JsonElement worldElement = object.get("world_name");
			if (worldElement == null) {
				if (mReferenceId != null) {
					mWorldRegex = null;
				} else {
					mWorldRegex = ".*";
				}
			} else {
				@Nullable String worldName = worldElement.getAsString();
				if (worldName == null || worldName.isEmpty()) {
					throw new Exception("Failed to parse 'world_name'");
				}
				mWorldRegex = worldName;
			}

			// Load whether this namespace is hidden by default on the dynmap
			@Nullable JsonElement hiddenElement = object.get("hidden");
			if (hiddenElement != null &&
				hiddenElement.getAsBoolean()) {
				mHidden = hiddenElement.getAsBoolean();
			}

			// Load the zones
			@Nullable JsonElement zonesElement = object.get("zones");
			if (zonesElement == null) {
				throw new Exception("Failed to parse 'zones'");
			}
			@Nullable JsonArray zonesArray = zonesElement.getAsJsonArray();
			if (zonesArray == null) {
				throw new Exception("Failed to parse 'zones'");
			}

			int zoneIndex = 0;

			for (JsonElement zoneElement : zonesArray) {
				@Nullable JsonObject zoneObject = zoneElement.getAsJsonObject();
				if (zoneObject == null) {
					throw new Exception("Failed to parse 'zones[" + zoneIndex + "]' as a json object");
				}

				mZoneOrder.add(zoneObject);
				String refId = getRefId(zoneObject);
				if (refId != null) {
					if (!mRequiredRefs.add(refId)) {
						throw new Exception("Reference ID " + refId + " used twice in namespace " + mName);
					}
				}

				zoneIndex++;
			}
		}

		protected List<JsonObject> resolve(Map<String, List<ZoneNamespaceFile>> references, @Nullable String parentWorldRegex) throws Exception {
			String worldRegex = parentWorldRegex;
			if (mWorldRegex != null) {
				worldRegex = mWorldRegex;
			}
			// This is a precaution; mWorldRegex is always set for main files, with a fallback of ".*"
			if (worldRegex == null) {
				worldRegex = ".*";
			}

			List<JsonObject> result = new ArrayList<>();

			for (JsonObject zoneOrRef : mZoneOrder) {
				String refId = getRefId(zoneOrRef);

				if (refId == null) {
					if (!zoneOrRef.has("world_name")) {
						zoneOrRef.addProperty("world_name", worldRegex);
					}
					result.add(zoneOrRef);
					continue;
				}

				List<ZoneNamespaceFile> refFiles = references.get(refId);
				if (refFiles != null) {
					for (ZoneNamespaceFile refFile : refFiles) {
						result.addAll(refFile.resolve(references, worldRegex));
					}
				} // Else case not required; handled by referenceCheck()
			}

			return result;
		}

		private @Nullable String getRefId(JsonObject zoneObject) throws Exception {
			JsonElement refElement = zoneObject.get("reference");
			if (refElement == null) {
				return null;
			}
			if (!(refElement instanceof JsonPrimitive refPrimitive) || !refPrimitive.isString()) {
				throw new Exception("\"#ref\" is not a string!");
			}
			String refId = refPrimitive.getAsString();
			if (refId.isEmpty()) {
				throw new Exception("\"#ref\" must not be empty!");
			}
			return refId;
		}
	}

	// Tracks references for a single namespace
	private static class NamespaceResolver {
		private final String mName;
		private final List<ZoneNamespaceFile> mMainFiles = new ArrayList<>();
		private final Map<String, List<ZoneNamespaceFile>> mRefs = new HashMap<>();

		protected NamespaceResolver(String name) {
			mName = name;
		}

		protected void addFile(ZoneNamespaceFile namespaceFile) {
			if (namespaceFile.mReferenceId == null) {
				mMainFiles.add(namespaceFile);
				return;
			}

			mRefs.computeIfAbsent(namespaceFile.mReferenceId, k -> new ArrayList<>()).add(namespaceFile);
		}

		protected ZoneNamespace resolve(Audience audience) throws Exception {
			if (mMainFiles.isEmpty()) {
				throw new Exception("Could not find main (non-reference) file for one ZoneNamespace " + mName + "!");
			}

			referenceCheck(audience);

			boolean hidden = mMainFiles.getFirst().mHidden;
			ZoneNamespace zoneNamespace = new ZoneNamespace(mName, hidden);

			for (ZoneNamespaceFile zoneNamespaceFile : mMainFiles) {
				for (JsonObject zoneObject : zoneNamespaceFile.resolve(mRefs, null)) {
					zoneNamespace.addZone(constructZoneFromJson(zoneNamespace, zoneObject));
				}
			}

			return zoneNamespace;
		}

		private void referenceCheck(Audience audience) throws Exception {
			if (mMainFiles.isEmpty()) {
				throw new Exception("No main (non-reference) file for ZoneNamespace " + mName);
			}

			TreeSet<String> toSearch = new TreeSet<>();
			for (ZoneNamespaceFile zoneNamespaceFile : mMainFiles) {
				toSearch.addAll(zoneNamespaceFile.mRequiredRefs);
			}
			Set<String> found = new HashSet<>();

			while (!toSearch.isEmpty()) {
				String refId = toSearch.pollFirst();
				if (!found.add(refId)) {
					throw new Exception("ZoneNamespace " + mName + " reference " + refId + " used multiple times!");
				}

				List<ZoneNamespaceFile> references = mRefs.get(refId);
				if (references == null) {
					throw new Exception("ZoneNamespace " + mName + " reference " + refId + " not found!");
				}

				for (ZoneNamespaceFile reference : references) {
					toSearch.addAll(reference.mRequiredRefs);
				}
			}

			for (String refId : mRefs.keySet()) {
				if (!found.contains(refId)) {
					audience.sendMessage(Component.text("ZoneNamespace " + mName + " reference " + refId
						+ " is unused", NamedTextColor.YELLOW));
				}
			}
		}
	}

	private static Zone constructZoneFromJson(ZoneNamespace namespace, JsonObject object) throws Exception {
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

		// This gets inserted from the ZoneNamespace file if missing from the zone JSON
		@Nullable JsonElement worldElement = object.get("world_name");
		if (worldElement == null) {
			throw new Exception("Failed to find inserted 'world_name'");
		}
		@Nullable String worldRegexStr = worldElement.getAsString();
		if (worldRegexStr == null || worldRegexStr.isEmpty()) {
			throw new Exception("Failed to parse 'world_name'");
		}

		// Load the zone location
		if (!(object.get("location") instanceof JsonObject locationJson)) {
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
		List<String> rawProperties = getRawZoneProperties(propertiesElement);
		Set<String> properties = Plugin.getInstance().mZonePropertyGroupManager.resolveProperties(namespace.getName(), rawProperties);

		return new Zone(namespace, worldRegexStr, pos1, pos2, name, properties);
	}

	public static List<String> getRawZoneProperties(@Nullable JsonElement propertiesElement) throws Exception {
		if (!(propertiesElement instanceof JsonArray propertiesArray)) {
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

	private final Audience mAudience;
	private final Set<String> mPluginNamespaces;
	private final Map<String, NamespaceResolver> mNamespaceResolvers = new HashMap<>();

	protected ZonesReferenceResolver(Plugin plugin, Audience audience, Set<String> otherPluginNamespaces) {
		mAudience = audience;
		mPluginNamespaces = otherPluginNamespaces;

		QuestUtils.loadScriptedQuests(plugin, "zone_namespaces", mAudience, (object) -> {
			// Load this file into a ZoneNamespaceFile object for further processing
			ZoneNamespaceFile namespaceFile = new ZoneNamespaceFile(object);
			if (mPluginNamespaces.contains(namespaceFile.mName)) {
				throw new Exception("Cannot use files for plugin-handled ZoneNamespaces");
			}

			mNamespaceResolvers.computeIfAbsent(namespaceFile.mName, NamespaceResolver::new)
				.addFile(namespaceFile);

			return null;
		});
	}

	public Map<String, ZoneNamespace> resolve() {
		Map<String, ZoneNamespace> result = new HashMap<>();

		for (Map.Entry<String, NamespaceResolver> entry : mNamespaceResolvers.entrySet()) {
			String namespaceId = entry.getKey();
			NamespaceResolver namespaceResolver = entry.getValue();

			try {
				result.put(namespaceId, namespaceResolver.resolve(mAudience));
			} catch (Exception ex) {
				mAudience.sendMessage(Component.text("Could not load ZoneNamespace " + namespaceId + "; skipping:"));
				MessagingUtils.sendStackTrace(mAudience, ex);
			}
		}

		return result;
	}
}
