package com.playmonumenta.scriptedquests.zones.zone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ZoneDefragmenter {
	class FragCombos extends HashMap<Set<Integer>, ZoneFragment> {}

	private Map<Integer, FragCombos> mMergedCombos = new HashMap<Integer, FragCombos>();
	private Set<Integer> mAllIds = new LinkedHashSet<Integer>();

	public ZoneDefragmenter(List<ZoneFragment> fragments) {
		FragCombos fragCombos = new FragCombos();
		mMergedCombos.put(1, fragCombos);
		Integer i = 0;
		for (ZoneFragment fragment : fragments) {
			// Individual fragments are groups of 1
			Set<Integer> mergedIds = new LinkedHashSet<Integer>();
			mergedIds.add(i);
			fragCombos.put(mergedIds, new ZoneFragment(fragment));

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
		FragCombos fragCombos = new FragCombos();
		mMergedCombos.put(mergeLevel, fragCombos);
		for (Integer lowerLevel = 1; lowerLevel <= mergeLevel/2; lowerLevel++) {
			Integer upperLevel = mergeLevel - lowerLevel;
			mergeTwoLevels(fragCombos, mergeLevel, lowerLevel, upperLevel);
		}
	}

	public void mergeTwoLevels(FragCombos fragCombos, Integer mergeLevel, Integer lowerLevel, Integer upperLevel) {
		// Previous code ensures null will not appear.
		FragCombos upperGroup = mMergedCombos.get(upperLevel);
		FragCombos lowerGroup = mMergedCombos.get(lowerLevel);
		for (Map.Entry<Set<Integer>, ZoneFragment> upperEntry : upperGroup.entrySet()) {
			Set<Integer> upperIds = upperEntry.getKey();
			ZoneFragment upperZone = upperEntry.getValue();
			for (Map.Entry<Set<Integer>, ZoneFragment> lowerEntry : lowerGroup.entrySet()) {
				Set<Integer> lowerIds = lowerEntry.getKey();
				ZoneFragment lowerZone = lowerEntry.getValue();

				Set<Integer> mergedIds = new LinkedHashSet<Integer>(lowerIds);
				mergedIds.addAll(upperIds);
				if (mergedIds.size() != mergeLevel) {
					// Some IDs were in common, so this isn't the merge_level we're looking for
					continue;
				}
				if (fragCombos.containsKey(mergedIds)) {
					// Same merged fragment already found
					continue;
				}

				ZoneFragment merged = upperZone.merge(lowerZone);
				if (merged == null) {
					// Couldn't merge, skip
					continue;
				}
				fragCombos.put(mergedIds, merged);
			}
		}
	}

	public List<ZoneFragment> optimalMerge() {
		List<ZoneFragment> resultsSoFar = new ArrayList<ZoneFragment>();
		return optimalMerge(resultsSoFar, mAllIds);
	}

	/*
		* Minimal zones are returned by searching for the largest merged zones first,
		* and returning the first result to have exactly one of each part.
		* In a worst case scenario, the original parts are returned.
		*
		* Returns the best solution (list of zones), or null (to continue searching).
		*/
	public List<ZoneFragment> optimalMerge(List<ZoneFragment> resultsSoFar, Set<Integer> remainingIds) {
		for (Integer mergeLevel = remainingIds.size(); mergeLevel >= 1; mergeLevel--) {
			FragCombos fragCombos = mMergedCombos.get(mergeLevel);
			for (Map.Entry<Set<Integer>, ZoneFragment> entry : fragCombos.entrySet()) {
				Set<Integer> mergedIds = entry.getKey();
				ZoneFragment mergedZone = entry.getValue();

				List<ZoneFragment> result = new ArrayList<ZoneFragment>(resultsSoFar);
				result.add(mergedZone);

				Set<Integer> overlappedIds = new LinkedHashSet<Integer>(mergedIds);
				overlappedIds.removeAll(remainingIds);
				if (!overlappedIds.isEmpty()) {
					// Overlap detected; not allowed even in the same ID
					continue;
				}

				Set<Integer> newRemaining = new LinkedHashSet<Integer>(remainingIds);
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
