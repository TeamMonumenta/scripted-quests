package com.playmonumenta.scriptedquests.zones;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.ZoneUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.util.Vector;
import javax.annotation.Nullable;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

public class ZoneLayer {
	public static final String DYNMAP_PREFIX = "SQZone";

	private String mName;
	private boolean mHidden = false;
	private List<Zone> mZones = new ArrayList<Zone>();
	private Set<String> mLoadedProperties = new HashSet<String>();

	/*
	 * This should only be called by the ZoneManager.
	 */
	public ZoneLayer(CommandSender sender, JsonObject object) throws Exception {
		this(new HashSet<CommandSender>(Arrays.asList(sender)), object);
	}

	public ZoneLayer(Set<CommandSender> senders, JsonObject object) throws Exception {
		if (object == null) {
			throw new Exception("object may not be null.");
		}

		// Load the layer name
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

		// Load whether this layer is hidden by default on the dynmap
		@Nullable JsonElement hiddenElement = object.get("hidden");
		if (hiddenElement != null &&
		    hiddenElement.getAsBoolean()) {
			mHidden = hiddenElement.getAsBoolean();
		}

		// Load the property groups - why yes, this section is rather long.
		@Nullable JsonElement propertyGroupsElement = object.get("property_groups");
		if (propertyGroupsElement == null) {
			throw new Exception("Failed to parse 'property_groups'");
		}
		@Nullable JsonObject propertyGroupsJson = propertyGroupsElement.getAsJsonObject();
		if (propertyGroupsJson == null) {
			throw new Exception("Failed to parse 'property_groups'");
		}

		Map<String, List<String>> propertyGroups = new HashMap<String, List<String>>();
		Map<String, Set<String>> groupReferences = new HashMap<String, Set<String>>();

		for (Map.Entry<String, JsonElement> ent : propertyGroupsJson.entrySet()) {
			String propertyGroupName = ent.getKey();
			JsonElement propertyGroupJson = ent.getValue();

			if (propertyGroupName == null || propertyGroupName.isEmpty()) {
				throw new Exception("Failed to parse 'property_groups': group name may not be empty.");
			}
			@Nullable JsonArray propertyGroupJsonArray = propertyGroupJson.getAsJsonArray();
			if (propertyGroupJsonArray == null) {
				throw new Exception("Failed to parse 'property_groups." + propertyGroupName + "'");
			}

			List<String> propertyGroup = new ArrayList<String>();
			Set<String> ownGroupReferences = new LinkedHashSet<String>();

			Integer propertyGroupIndex = 0;
			Iterator<JsonElement> propertyGroupIter = propertyGroupJsonArray.iterator();

			while (propertyGroupIter.hasNext()) {
				JsonElement propertyNameElement = propertyGroupIter.next();
				String propertyName = propertyNameElement.getAsString();

				if (propertyName == null) {
					throw new Exception("Failed to parse 'property_groups." + propertyGroupName +
					                    "[" + Integer.toString(propertyGroupIndex) + "]'");
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
				throw new Exception("Loop detected in property group '" + propertyGroupName +
				                    "'. Groups may not reference themselves directly or indirectly.");
			}

			propertyGroups.put(propertyGroupName, propertyGroup);
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

		Integer zoneIndex = 0;
		Iterator<JsonElement> zonesIter = zonesArray.iterator();

		while (zonesIter.hasNext()) {
			JsonElement zoneElement = zonesIter.next();
			@Nullable JsonObject zoneObject = zoneElement.getAsJsonObject();
			if (zoneObject == null) {
				throw new Exception("Failed to parse 'zones[" + Integer.toString(zoneIndex) + "]'");
			}
			Zone zone = Zone.constructFromJson(this, zoneObject, propertyGroups);
			mLoadedProperties.addAll(zone.getProperties());
			mZones.add(zone);
			zoneIndex++;
		}

		reloadFragments(senders);
	}

	/************************************************************************************
	 * Start of methods for use with external plugins:
	 ************************************************************************************/

	/*
	 * Create an empty zone layer for use with external plugins.
	 *
	 * name is the name of the layer. This should start with your plugin's name or ID.
	 */
	public ZoneLayer(String name) {
		mName = name;
	}

	public ZoneLayer(String name, boolean hidden) {
		mName = name;
		mHidden = hidden;
	}

	/*
	 * Interface to add a zone from external plugins. First zone added has the highest priority.
	 *
	 * pos1 and pos2 define the bounds of the zone, similar to /fill. Order doesn't matter.
	 * name is the name of the zone.
	 * properties is the set of properties for the zone.
	 *
	 * Property group support is not provided for this method. Your plugin will need to
	 * handle that on its own.
	 */
	public boolean addZone(Vector pos1, Vector pos2, String name, Set<String> properties) {
		@Nullable Zone zone = null;

		try {
			zone = new Zone(this, pos1, pos2, name, properties);
		} catch (Exception e) {
			return false;
		}

		return addZone(zone);
	}

	/*
	 * Interface to add a zone from external plugins. First zone added has the highest priority.
	 */
	public boolean addZone(Zone zone) {
		return mZones.add(zone);
	}

	/*
	 * Create a zone tree containing just this layer.
	 *
	 * Note that creating multiple zone trees from the same layer,
	 * including registering it with the ZoneManager, will invalidate
	 * the previous trees from that layer. This will not be detected
	 * or handled automatically, so please be careful.
	 *
	 * Returns a subclass of ZoneTreeBase.
	 */
	public ZoneTreeBase createZoneTree(CommandSender sender) throws Exception {
		Set<CommandSender> senders = new HashSet<CommandSender>();
		senders.add(sender);
		return createZoneTree(senders);
	}

	public ZoneTreeBase createZoneTree(Set<CommandSender> senders) throws Exception {
		reloadFragments(senders);

		// Create list of all zone fragments.
		List<ZoneFragment> zoneFragments = new ArrayList<ZoneFragment>();
		for (Zone zone : mZones) {
			zoneFragments.addAll(zone.getZoneFragments());
		}

		// Create the new tree.
		ZoneTreeBase newTree = ZoneTreeBase.createZoneTree(zoneFragments);
		if (Plugin.getInstance().mShowZonesDynmap) {
			newTree.refreshDynmapTree();
		}
		return newTree;
	}

	/*
	 * Return the list of properties found in this zone layer.
	 */
	public Set<String> getLoadedProperties() {
		return new HashSet<String>(mLoadedProperties);
	}

	/************************************************************************************
	 * End of methods for use with external plugins:
	 ************************************************************************************/

	/*
	 * Reset the fragments of this ZoneLayer so they can be recalculated without reloading the zones.
	 * Used to handle ZoneLayers from other plugins. This should only be called by the ZoneManager
	 * and the ZoneLayer constructor.
	 */
	protected void reloadFragments(CommandSender sender) {
		Set<CommandSender> senders = new HashSet<CommandSender>();
		senders.add(sender);
		reloadFragments(senders);
	}

	protected void reloadFragments(Set<CommandSender> senders) {
		for (Zone zone : mZones) {
			zone.reloadFragments();
		}

		// Split the zones into non-overlapping fragments
		removeOverlaps(senders, mZones);

		if (Plugin.getInstance().mShowZonesDynmap) {
			refreshDynmapLayer();
		}
	}

	/*
	 * Force all zones to discard their current fragments. The fragments will
	 * continue to reference their parent zones after this.
	 *
	 * Not strictly required, but improves garbage collection by removing loops
	 *
	 * This should only be called by the ZoneManager.
	 */
	public void invalidate() {
		for (Zone zone : mZones) {
			zone.invalidate();
		}
	}

	public String getName() {
		return mName;
	}

	public List<Zone> getZones() {
		List<Zone> result = new ArrayList<Zone>();
		result.addAll(mZones);
		return result;
	}

	/*
	 * Use this only as a fallback; it's slower than using a zone tree.
	 */
	public @Nullable Zone fallbackGetZone(Vector loc) {
		for (Zone zone : mZones) {
			if (zone.within(loc)) {
				return zone;
			}
		}

		return null;
	}

	private void removeOverlaps(Set<CommandSender> senders, List<Zone> zones) {
		for (int i = 0; i < zones.size(); i++) {
			Zone outer = zones.get(i);
			for (Zone inner : zones.subList(i + 1, zones.size())) {
				@Nullable ZoneBase overlap = outer.overlappingZone(inner);
				if (overlap == null) {
					continue;
				}
				if (inner.splitByOverlap(overlap, outer)) {
					for (@Nullable CommandSender sender : senders) {
						if (sender != null) {
							sender.sendMessage(Component.text("Total eclipse of zone ", NamedTextColor.RED)
								.append(Component.text(inner.getName(), NamedTextColor.RED, TextDecoration.BOLD))
								.append(Component.text(" by zone ", NamedTextColor.RED))
								.append(Component.text(outer.getName(), NamedTextColor.RED, TextDecoration.BOLD)));
						}
					}
				}
				outer.splitByOverlap(overlap, inner, true);
			}
		}
	}

	private boolean hasPropertyGroupLoop(Map<String, Set<String>> groupReferences, String startGroup) {
		return hasPropertyGroupLoop(groupReferences, startGroup, startGroup);
	}

	private boolean hasPropertyGroupLoop(Map<String, Set<String>> groupReferences, String startGroup, String continueGroup) {
		@Nullable Set<String> subGroupReferences = groupReferences.get(continueGroup);
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

	/*
	 * This should only be called by the ZoneManager.
	 */
	protected static void clearDynmapLayers() {
		@Nullable DynmapCommonAPI dynmapHook = (DynmapCommonAPI) Bukkit.getServer().getPluginManager().getPlugin("dynmap");
		if (dynmapHook == null) {
			return;
		}

		@Nullable MarkerAPI markerHook = dynmapHook.getMarkerAPI();
		if (markerHook == null) {
			// Not initialized
			return;
		}

		// Duplicate set of MarkerSets in case the return value is a view, not a copy.
		Set<MarkerSet> allMarkerSets = new LinkedHashSet<MarkerSet>();
		allMarkerSets.addAll(markerHook.getMarkerSets());
		for (MarkerSet markerSet : allMarkerSets) {
			if (markerSet != null &&
			    markerSet.getMarkerSetID().startsWith(DYNMAP_PREFIX) &&
			    !markerSet.isMarkerSetPersistent()) {
				markerSet.deleteMarkerSet();
			}
		}
	}

	private void refreshDynmapLayer() {
		@Nullable DynmapCommonAPI dynmapHook = (DynmapCommonAPI) Bukkit.getServer().getPluginManager().getPlugin("dynmap");
		if (dynmapHook == null) {
			return;
		}

		@Nullable MarkerAPI markerHook = dynmapHook.getMarkerAPI();
		if (markerHook == null) {
			// Not initialized
			return;
		}

		String markerSetId = DYNMAP_PREFIX + mName.replace(" ", "_");
		@Nullable MarkerSet markerSet;

		markerSet = markerHook.getMarkerSet(markerSetId);
		if (markerSet != null) {
			// Delete old marker set
			markerSet.deleteMarkerSet();
		}
		// Create a new marker set
		markerSet = markerHook.createMarkerSet(markerSetId, mName, null, false);

		// Mark hidden if needed
		markerSet.setHideByDefault(mHidden);

		// Zones reversed so clicking on the overlap of two zones returns the highest priority zone.
		// This isn't 100% consistent either way, but it's more consistent like this without needing
		// to render every zone fragment (which is also an option).
		for (int zoneIndex = mZones.size() - 1; zoneIndex >= 0; zoneIndex--) {
			Zone zone = mZones.get(zoneIndex);

			String zoneLabel = zone.getName();
			int zoneColor = ZoneUtils.getColor(mName, zoneLabel);
			String zoneId = zoneLabel.replace(" ", "_");

			// TODO Replace the first false in createAreaMarker with true and make zoneLabel HTML markup here.

			String world = Bukkit.getWorlds().get(0).getName();

			Vector minCorner = zone.minCorner();
			Vector maxCorner = zone.maxCornerExclusive();

			double[] x = new double[2];
			double[] z = new double[2];
			x[0] = minCorner.getX();
			z[0] = minCorner.getZ();
			x[1] = maxCorner.getX();
			z[1] = maxCorner.getZ();

			@Nullable AreaMarker areaMarker = markerSet.createAreaMarker(zoneId, zoneLabel, false, world, x, z, false);
			if (areaMarker != null) {
				areaMarker.setRangeY(maxCorner.getY(), minCorner.getY());
				areaMarker.setFillStyle(0.2, zoneColor);
				areaMarker.setLineStyle(1, 0.3, zoneColor);
			}
		}
	}
}
