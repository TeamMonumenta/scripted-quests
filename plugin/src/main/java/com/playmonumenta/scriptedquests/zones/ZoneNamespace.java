package com.playmonumenta.scriptedquests.zones;

import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

public class ZoneNamespace {
	public static final String DYNMAP_PREFIX = "SQZone";

	private final String mName;
	private boolean mHidden = false;
	private final List<Zone> mZones = new ArrayList<>();
	private final Set<String> mLoadedProperties = new TreeSet<>();

	/*
	 * This should only be called by the ZoneManager.
	 */
	protected ZoneNamespace(String name, boolean isHidden, List<JsonObject> zoneObjects) throws Exception {
		mName = name;
		mHidden = isHidden;
		for (JsonObject zoneObject : zoneObjects) {
			Zone zone = Zone.constructFromJson(this, zoneObject);
			mLoadedProperties.addAll(zone.getProperties());
			mZones.add(zone);
		}

		refreshDynmapLayer();
	}

	/************************************************************************************
	 * Start of methods for use with external plugins:
	 ************************************************************************************/

	/*
	 * Create an empty zone namespace for use with external plugins.
	 *
	 * name is the name of the namespace. This should start with your plugin's name or ID.
	 */
	public ZoneNamespace(String name) {
		mName = name;
	}

	public ZoneNamespace(String name, boolean hidden) {
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
	public boolean addZone(String worldRegex, Vector pos1, Vector pos2, String name, Set<String> properties) {
		@Nullable Zone zone;

		try {
			zone = new Zone(this, worldRegex, pos1, pos2, name, properties);
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
	 * Return the list of properties found in this zone namespace.
	 */
	public Set<String> getLoadedProperties() {
		return Collections.unmodifiableSet(mLoadedProperties);
	}

	/************************************************************************************
	 * End of methods for use with external plugins:
	 ************************************************************************************/

	public String getName() {
		return mName;
	}

	public List<Zone> getZones() {
		return Collections.unmodifiableList(mZones);
	}

	/*
	 * Use this only as a fallback; it's slower than using a zone tree.
	 */
	public @Nullable Zone fallbackGetZoneLegacy(Vector loc) {
		for (Zone zone : mZones) {
			if (zone.within(loc)) {
				return zone;
			}
		}

		return null;
	}

	public @Nullable Zone fallbackGetZone(Location loc) {
		for (Zone zone : mZones) {
			if (zone.within(loc)) {
				return zone;
			}
		}

		return null;
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
		Set<MarkerSet> allMarkerSets = new LinkedHashSet<>(markerHook.getMarkerSets());
		for (MarkerSet markerSet : allMarkerSets) {
			if (markerSet != null &&
			    markerSet.getMarkerSetID().startsWith(DYNMAP_PREFIX) &&
			    !markerSet.isMarkerSetPersistent()) {
				markerSet.deleteMarkerSet();
			}
		}
	}

	protected void refreshDynmapLayer() {
		if (!Plugin.getInstance().mShowZonesDynmap) {
			return;
		}

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
