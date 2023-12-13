package com.playmonumenta.scriptedquests.zones;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class WorldRegexMatcher {
	private static Map<String, Pattern> mPatterns = new HashMap<>();
	private static Map<World, Set<String>> mWorldPatternMatches = new HashMap<>();

	protected WorldRegexMatcher(Set<String> worldRegexes) throws PatternSyntaxException {
		for (String worldRegexStr : worldRegexes) {
			mPatterns.put(worldRegexStr, Pattern.compile(worldRegexStr));
		}

		for (World world : Bukkit.getWorlds()) {
			onLoadWorld(world);
		}
	}

	protected void onLoadWorld(World world) {
		Set<String> matchingPatterns = new HashSet<>();
		mWorldPatternMatches.put(world, matchingPatterns);

		String worldName = world.getName();
		for (Map.Entry<String, Pattern> entry : mPatterns.entrySet()) {
			Pattern pattern = entry.getValue();

			// Must match the entire string to proceed
			if (!pattern.matcher(worldName).matches()) {
				continue;
			}

			String patternId = entry.getKey();
			matchingPatterns.add(patternId);
		}
	}

	protected void onUnloadWorld(World world) {
		mWorldPatternMatches.remove(world);
	}

	// Only works for the patterns provided at matcher instantiation
	protected boolean matches(World world, String worldRegex) {
		Set<String> matches = mWorldPatternMatches.get(world);
		if (matches == null) {
			return false;
		}
		return matches.contains(worldRegex);
	}
}
