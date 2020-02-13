package com.playmonumenta.scriptedquests.zones;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.util.Vector;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.ZoneUtils;
import com.playmonumenta.scriptedquests.zones.zone.BaseZone;
import com.playmonumenta.scriptedquests.zones.zone.Zone;
import com.playmonumenta.scriptedquests.zones.zone.ZoneFragment;
import com.playmonumenta.scriptedquests.zones.zonetree.BaseZoneTree;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;

public class ZoneLayer<T> {
	public static final String DYNMAP_PREFIX = "SQZone";

	private String mName;
	private ArrayList<Zone<T>> mZones = new ArrayList<Zone<T>>();

	/*
	 * This should only be called by the ZoneManager.
	 */
	public ZoneLayer(CommandSender sender, JsonObject object) throws Exception {
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
			mZones.add(Zone.constructFromJson(this, zoneElement.getAsJsonObject(), propertyGroups, (T)null));
			zoneIndex++;
		}

		reloadFragments(sender);
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

	/*
	 * Interface to add a zone from external plugins. First zone added has the highest priority.
	 *
	 * pos1 and pos2 define the bounds of the zone, similar to /fill. Order doesn't matter.
	 * name is the name of the zone.
	 * properties is the set of properties for the zone.
	 * tag is an object that the calling plugin would like to be associated
	 *   with the zone for easy reference later
	 *
	 * Property group support is not provided for this method. Your plugin will need to
	 * handle that on its own.
	 */
	public boolean addZone(Vector pos1, Vector pos2, String name, LinkedHashSet<String> properties, T tag) {
		Zone<T> zone = null;

		try {
			zone = new Zone<T>(this, pos1, pos2, name, properties, tag);
		} catch (Exception e) {
			return false;
		}

		if (mZones.add(zone)) {
			return true;
		}
		return false;
	}

	/*
	 * Create a zone tree containing just this layer.
	 *
	 * Note that creating multiple zone trees from the same layer,
	 * including registering it with the ZoneManager, will invalidate
	 * the previous trees from that layer. This will not be detected
	 * or handled automatically, so please be careful.
	 *
	 * Returns a subclass of BaseZoneTree.
	 */
	public BaseZoneTree<T> createZoneTree(CommandSender sender) throws Exception {
		reloadFragments(sender);

		// Create list of all zone fragments.
		ArrayList<ZoneFragment<T>> zoneFragments = new ArrayList<ZoneFragment<T>>();
		for (Zone<T> zone : mZones) {
			zoneFragments.addAll(zone.getZoneFragments());
		}

		// Create the new tree.
		return BaseZoneTree.CreateZoneTree(sender, zoneFragments);
	}

	/************************************************************************************
	 * End of methods for use with external plugins:
	 ************************************************************************************/

	/*
	 * Reset the fragments of this ZoneLayer so they can be recalculated without reloading the zones.
	 * Used to handle ZoneLayers from other plugins. This should only be called by the ZoneManager
	 * and the ZoneLayer constructor.
	 */
	public void reloadFragments(CommandSender sender) throws Exception {
		for (Zone<T> zone : mZones) {
			zone.reloadFragments();
		}

		// Split the zones into non-overlapping fragments
		removeOverlaps(sender, mZones);

		// TODO Defragment to reduce fragment count (approx 2-3x on average)

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
		for (Zone<T> zone : mZones) {
			zone.invalidate();
		}
	}

	public String getName() {
		return mName;
	}

	public ArrayList<Zone<T>> getZones() {
		ArrayList<Zone<T>> result = new ArrayList<Zone<T>>();
		result.addAll(mZones);
		return result;
	}

	private void removeOverlaps(CommandSender sender, ArrayList<Zone<T>> zones) throws Exception {
		for (int i = 0; i < zones.size(); i++) {
			Zone<T> outer = zones.get(i);
			for (Zone<T> inner : zones.subList(i + 1, zones.size())) {
				BaseZone overlap = outer.overlappingZone(inner);
				if (overlap == null) {
					continue;
				}
				if (inner.splitByOverlap(overlap, outer)) {
					String errorMessage = ChatColor.RED + "Total eclipse of zone "
										+ ChatColor.BOLD + inner.getName()
										+ ChatColor.RED + " by zone "
										+ ChatColor.BOLD + outer.getName();
					sender.spigot().sendMessage(TextComponent.fromLegacyText(errorMessage));
				}
				outer.splitByOverlap(overlap, inner, true);
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

	/*
	 * This should only be called by the ZoneManager.
	 */
	public static void clearDynmapLayers() {
		DynmapCommonAPI dynmapHook = (DynmapCommonAPI) Bukkit.getServer().getPluginManager().getPlugin("dynmap");
		if (dynmapHook == null) {
			return;
		}

		MarkerAPI markerHook = dynmapHook.getMarkerAPI();
		if (markerHook == null) {
			// Not initialized
			return;
		}

		// Duplicate set of MarkerSets in case the return value is a view, not a copy.
		LinkedHashSet<MarkerSet> allMarkerSets = new LinkedHashSet<MarkerSet>();
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
		DynmapCommonAPI dynmapHook = (DynmapCommonAPI) Bukkit.getServer().getPluginManager().getPlugin("dynmap");
		if (dynmapHook == null) {
			return;
		}

		MarkerAPI markerHook = dynmapHook.getMarkerAPI();
		if (markerHook == null) {
			// Not initialized
			return;
		}

		String markerSetId = DYNMAP_PREFIX + mName.replace(" ", "_");
		MarkerSet markerSet;

		markerSet = markerHook.getMarkerSet(markerSetId);
		if (markerSet != null) {
			// Delete old marker set
			markerSet.deleteMarkerSet();
		}
		// Create a new marker set
		markerSet = markerHook.createMarkerSet(markerSetId, mName, null, false);

		// Zones reversed so clicking on the overlap of two zones returns the highest priority zone.
		// This isn't 100% consistent either way, but it's more consistent like this without needing
		// to render every zone fragment (which is also an option).
		for (int zoneIndex = mZones.size() - 1; zoneIndex >= 0; zoneIndex--) {
			Zone<T> zone = mZones.get(zoneIndex);

			String zoneLabel = zone.getName();
			int zoneColor = ZoneUtils.getColor(mName, zoneLabel);
			String zoneId = zoneLabel.replace(" ", "_");

			// TODO Replace the first false in createAreaMarker with true and make zoneLabel HTML markup here.

			String world = Bukkit.getWorlds().get(0).getName();

			Vector minCorner = zone.minCorner();
			Vector maxCorner = zone.maxCornerExclusive();

			double x[] = new double[2];
			double z[] = new double[2];
			x[0] = minCorner.getX();
			z[0] = minCorner.getZ();
			x[1] = maxCorner.getX();
			z[1] = maxCorner.getZ();

			AreaMarker areaMarker = markerSet.createAreaMarker(zoneId, zoneLabel, false, world, x, z, false);
			areaMarker.setRangeY(maxCorner.getY(), minCorner.getY());
			areaMarker.setFillStyle(0.2, zoneColor);
			areaMarker.setLineStyle(1, 0.3, zoneColor);
		}
	}
}
