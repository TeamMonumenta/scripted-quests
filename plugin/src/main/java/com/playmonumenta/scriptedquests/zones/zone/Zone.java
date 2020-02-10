package com.playmonumenta.scriptedquests.zones.zone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
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
	private ArrayList<ZoneFragment<T>> mFragments = new ArrayList<ZoneFragment<T>>();
	private final LinkedHashSet<String> mProperties = new LinkedHashSet<String>();
	private final T mTag;

	public static <T> Zone<T> constructFromJson(ZoneLayer<T> layer, JsonObject object, HashMap<String, ArrayList<String>> propertyGroups, T tag) throws Exception {
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
		Set<Entry<String, JsonElement>> entries = locationJson.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
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

		ZoneFragment<T> initialFragment = new ZoneFragment<T>(this);
		mFragments.add(initialFragment);
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

	/*
	 * Split all fragments of this zone by an overlapping zone, removing overlap.
	 */
	public boolean splitByOverlap(BaseZone overlap, Zone otherZone) {
		return splitByOverlap(overlap, otherZone, false);
	}

	public T getTag() {
		return mTag;
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
		ArrayList<ZoneFragment<T>> newFragments = new ArrayList<ZoneFragment<T>>();
		for (ZoneFragment<T> fragment : mFragments) {
			BaseZone subOverlap = fragment.overlappingZone(overlap);

			if (subOverlap == null)
			{
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

		class Defragmenter<T> {
			class FragCombos<T> {
				public HashMap<LinkedHashSet<Integer>, ZoneFragment<T>> mCombos;

				public FragCombos() {
					mCombos = new HashMap<LinkedHashSet<Integer>, ZoneFragment<T>>();
				}
			}

			private HashMap<Integer, FragCombos<T>> mMergedCombos = new HashMap<Integer, FragCombos<T>>();
			private LinkedHashSet<Integer> mAllIds = new LinkedHashSet<Integer>();

			public Defragmenter(ArrayList<ZoneFragment<T>> fragments) {
				FragCombos<T> fragCombos = new FragCombos<T>();
				mMergedCombos.put(1, fragCombos);
				Integer i = 0;
				for (ZoneFragment<T> fragment : fragments) {
					// Individual fragments are groups of 1
					LinkedHashSet<Integer> mergedIds = new LinkedHashSet<Integer>();
					mergedIds.add(i);
					fragCombos.mCombos.put(mergedIds, new ZoneFragment<T>(fragment));

					// We'll need a set of all IDs later.
					mAllIds.add(i);

					i++;
				}

				/*
				 * Get all possible mMergedCombos of parts; start at 2 (having completed 1) and count to the max size
				 * mergeLevel is the number of fragments in a grouped zone.
				 * For example, if A and B are original fragments (level 1),
				 * and C = A + B, C is level 2 (contains 2 original fragments).
				 * If D = C + A, D is level 3 (upperLevel = 2, lowerLevel = 1, 2 + 1)
				 */
				 for (Integer mergeLevel = 2; mergeLevel <= fragments.size(); mergeLevel++) {
					mergeAtLevel(mergeLevel);
				 }
			}

			public void mergeAtLevel(Integer mergeLevel) {
				FragCombos<T> fragCombos = new FragCombos<T>();
				mMergedCombos.put(mergeLevel, fragCombos);
				for (Integer lowerLevel = 2; lowerLevel <= mergeLevel/2; lowerLevel++) {
					Integer upperLevel = mergeLevel - lowerLevel;
					mergeTwoLevels(fragCombos, mergeLevel, lowerLevel, upperLevel);
				}
			}

			public void mergeTwoLevels(FragCombos<T> fragCombos, Integer mergeLevel, Integer lowerLevel, Integer upperLevel) {
				// Previous code ensures null will not appear.
				FragCombos<T> upperGroup = mMergedCombos.get(upperLevel);
				FragCombos<T> lowerGroup = mMergedCombos.get(lowerLevel);
				for (Entry<LinkedHashSet<Integer>, ZoneFragment<T>> upperEntry : upperGroup.mCombos.entrySet()) {
					LinkedHashSet<Integer> upperIds = upperEntry.getKey();
					ZoneFragment<T> upperZone = upperEntry.getValue();
					for (Entry<LinkedHashSet<Integer>, ZoneFragment<T>> lowerEntry : lowerGroup.mCombos.entrySet()) {
						LinkedHashSet<Integer> lowerIds = lowerEntry.getKey();
						ZoneFragment<T> lowerZone = lowerEntry.getValue();

						LinkedHashSet<Integer> mergedIds = new LinkedHashSet<Integer>();
						mergedIds.addAll(lowerIds);
						mergedIds.addAll(upperIds);
						if (mergedIds.size() != mergeLevel) {
							// Some IDs were in common, so this isn't the merge_level we're looking for
							continue;
						}
						if (fragCombos.mCombos.containsKey(mergedIds)) {
							// Same merged fragment already found
							continue;
						}

						ZoneFragment<T> merged = upperZone.merge(lowerZone);
						if (merged == null) {
							// Couldn't merge, skip
							continue;
						}
						fragCombos.mCombos.put(mergedIds, merged);
					}
				}
			}

			public ArrayList<ZoneFragment<T>> optimalMerge() {
				ArrayList<ZoneFragment<T>> resultsSoFar = new ArrayList<ZoneFragment<T>>();
				LinkedHashSet<Integer> remainingIds = new LinkedHashSet<Integer>();
				remainingIds.addAll(mAllIds);
				return optimalMerge(resultsSoFar, mAllIds);
			}

			/*
			 * Minimal zones are returned by searching for the largest merged zones first,
			 * and returning the first result to have exactly one of each part.
			 * In a worst case scenario, the original parts are returned.
			 *
			 * Returns the best solution (list of zones), or null (to continue searching).
			 */
			public ArrayList<ZoneFragment<T>> optimalMerge(ArrayList<ZoneFragment<T>> resultsSoFar, LinkedHashSet<Integer> remainingIds) {
				for (Integer mergeLevel = remainingIds.size(); mergeLevel >= 0; mergeLevel--) {
					FragCombos<T> fragCombos = mMergedCombos.get(mergeLevel);
					for (Entry<LinkedHashSet<Integer>, ZoneFragment<T>> entry : fragCombos.mCombos.entrySet()) {
						LinkedHashSet<Integer> mergedIds = entry.getKey();
						ZoneFragment<T> mergedZone = entry.getValue();

						ArrayList<ZoneFragment<T>> result = new ArrayList<ZoneFragment<T>>();
						result.addAll(resultsSoFar);
						result.add(mergedZone);

						LinkedHashSet<Integer> overlappedIds = new LinkedHashSet<Integer>();
						overlappedIds.addAll(mergedIds);
						overlappedIds.removeAll(remainingIds);
						if (!overlappedIds.isEmpty()) {
							// Overlap detected; not allowed even in the same ID
							continue;
						}

						LinkedHashSet<Integer> newRemaining = new LinkedHashSet<Integer>();
						newRemaining.addAll(remainingIds);
						newRemaining.removeAll(mergedIds);
						if (newRemaining.isEmpty()) {
							//Best possible result!
							return result;
						}

						result = optimalMerge(result, newRemaining);
						if (result != null) {
							// That recursion got the best result!
							return result;
						}

						// Oh, ok. Keep searching the next level down then.
					}
				}

				// None found with this recursion
				return null;
			}
		}

		// Load current fragments into defragmenter
		Defragmenter<T> defragmenter = new Defragmenter<T>(mFragments);

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

	public ArrayList<ZoneFragment<T>> getZoneFragments() {
		ArrayList<ZoneFragment<T>> result = new ArrayList<ZoneFragment<T>>();
		result.addAll(mFragments);
		return result;
	}

	public Set<String> getProperties() {
		Set<String> result = new LinkedHashSet<String>();
		result.addAll(mProperties);
		return result;
	}

	public boolean hasProperty(String propertyName) {
		return mProperties.contains(propertyName);
	}

	private static void applyProperty(HashMap<String, ArrayList<String>> propertyGroups, Set<String> currentProperties, String propertyName) throws Exception {
		applyProperty(propertyGroups, currentProperties, propertyName, false);
	}

	private static void applyProperty(HashMap<String, ArrayList<String>> propertyGroups, Set<String> currentProperties, String propertyName, boolean remove) throws Exception {
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
			ArrayList<String> propertyGroup = propertyGroups.get(propertyName.substring(1));
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

	@Override
	public int hashCode() {
		int result = ((BaseZone) this).hashCode();
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
