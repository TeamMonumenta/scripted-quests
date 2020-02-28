package com.playmonumenta.scriptedquests.zones.zone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ZoneDefragmenter<T> {
	class FragCombos<T> extends HashMap<Set<Integer>, ZoneFragment<T>> {}

	private Map<Integer, FragCombos<T>> mMergedCombos = new HashMap<Integer, FragCombos<T>>();
	private Set<Integer> mAllIds = new LinkedHashSet<Integer>();

	public ZoneDefragmenter(List<ZoneFragment<T>> fragments) {
		FragCombos<T> fragCombos = new FragCombos<T>();
		mMergedCombos.put(1, fragCombos);
		Integer i = 0;
		for (ZoneFragment<T> fragment : fragments) {
			// Individual fragments are groups of 1
			Set<Integer> mergedIds = new LinkedHashSet<Integer>();
			mergedIds.add(i);
			fragCombos.put(mergedIds, new ZoneFragment<T>(fragment));

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
		for (Integer lowerLevel = 1; lowerLevel <= mergeLevel/2; lowerLevel++) {
			Integer upperLevel = mergeLevel - lowerLevel;
			mergeTwoLevels(fragCombos, mergeLevel, lowerLevel, upperLevel);
		}
	}

	public void mergeTwoLevels(FragCombos<T> fragCombos, Integer mergeLevel, Integer lowerLevel, Integer upperLevel) {
		// Previous code ensures null will not appear.
		FragCombos<T> upperGroup = mMergedCombos.get(upperLevel);
		FragCombos<T> lowerGroup = mMergedCombos.get(lowerLevel);
		for (Map.Entry<Set<Integer>, ZoneFragment<T>> upperEntry : upperGroup.entrySet()) {
			Set<Integer> upperIds = upperEntry.getKey();
			ZoneFragment<T> upperZone = upperEntry.getValue();
			for (Map.Entry<Set<Integer>, ZoneFragment<T>> lowerEntry : lowerGroup.entrySet()) {
				Set<Integer> lowerIds = lowerEntry.getKey();
				ZoneFragment<T> lowerZone = lowerEntry.getValue();

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

				ZoneFragment<T> merged = upperZone.merge(lowerZone);
				if (merged == null) {
					// Couldn't merge, skip
					continue;
				}
				fragCombos.put(mergedIds, merged);
			}
		}
	}

	public List<ZoneFragment<T>> optimalMerge() {
		List<ZoneFragment<T>> resultsSoFar = new ArrayList<ZoneFragment<T>>();
		Set<Integer> remainingIds = new LinkedHashSet<Integer>(mAllIds);
		return optimalMerge(resultsSoFar, mAllIds);
	}

	/*
		* Minimal zones are returned by searching for the largest merged zones first,
		* and returning the first result to have exactly one of each part.
		* In a worst case scenario, the original parts are returned.
		*
		* Returns the best solution (list of zones), or null (to continue searching).
		*/
	public List<ZoneFragment<T>> optimalMerge(List<ZoneFragment<T>> resultsSoFar, Set<Integer> remainingIds) {
		for (Integer mergeLevel = remainingIds.size(); mergeLevel >= 0; mergeLevel--) {
			FragCombos<T> fragCombos = mMergedCombos.get(mergeLevel);
			for (Map.Entry<Set<Integer>, ZoneFragment<T>> entry : fragCombos.entrySet()) {
				Set<Integer> mergedIds = entry.getKey();
				ZoneFragment<T> mergedZone = entry.getValue();

				List<ZoneFragment<T>> result = new ArrayList<ZoneFragment<T>>(resultsSoFar);
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
