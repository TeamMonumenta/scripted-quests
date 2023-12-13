package com.playmonumenta.scriptedquests.zones;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
import javax.annotation.Nullable;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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

		protected List<JsonObject> resolve(Map<String, ZoneNamespaceFile> references, @Nullable String parentWorldRegex) throws Exception {
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

				ZoneNamespaceFile reference = references.get(refId);
				if (reference != null) {
					result.addAll(reference.resolve(references, worldRegex));
				} // Else case not required; handled by referenceCheck()
			}

			return result;
		}

		private @Nullable String getRefId(JsonObject zoneObject) throws Exception {
			JsonElement refElement = zoneObject.get("#ref");
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
		private final Map<String, ZoneNamespaceFile> mRefs = new HashMap<>();

		protected NamespaceResolver(String name) {
			mName = name;
		}

		protected void addFile(ZoneNamespaceFile namespaceFile) throws Exception {
			if (namespaceFile.mReferenceId == null) {
				mMainFiles.add(namespaceFile);
				return;
			}

			if (mRefs.put(namespaceFile.mReferenceId, namespaceFile) != null) {
				throw new Exception("Detected multiple files for one ZoneNamespace with reference ID "
					+ namespaceFile.mReferenceId + "!");
			}
		}

		protected ZoneNamespace resolve(Audience audience) throws Exception {
			if (mMainFiles.isEmpty()) {
				throw new Exception("Could not find main (non-reference) file for one ZoneNamespace " + mName + "!");
			}

			referenceCheck(audience);

			List<JsonObject> zoneObjects = new ArrayList<>();
			for (ZoneNamespaceFile zoneNamespaceFile : mMainFiles) {
				zoneObjects.addAll(zoneNamespaceFile.resolve(mRefs, null));
			}

			return new ZoneNamespace(mName, mMainFiles.get(0).mHidden, zoneObjects);
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

				ZoneNamespaceFile reference = mRefs.get(refId);
				if (reference == null) {
					throw new Exception("ZoneNamespace " + mName + " reference " + refId + " not found!");
				}

				toSearch.addAll(reference.mRequiredRefs);
			}

			for (String refId : mRefs.keySet()) {
				if (!found.contains(refId)) {
					audience.sendMessage(Component.text("ZoneNamespace " + mName + " reference " + refId
						+ " is unused", NamedTextColor.YELLOW));
				}
			}
		}
	}

	private final Audience mAudience;
	private final Set<String> mPluginNamespaces;
	private final Map<String, NamespaceResolver> mNamespaceResolvers = new HashMap<>();

	protected ZonesReferenceResolver(Plugin plugin, Audience audience, Set<String> pluginNamespaces) {
		mAudience = audience;
		mPluginNamespaces = pluginNamespaces;

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
