package com.playmonumenta.scriptedquests.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class WorldRegexMatcher {
	private final Map<String, Pattern> mPatterns = new HashMap<>();
	private final Map<String, Set<String>> mWorldPatternMatches = new HashMap<>();

	public WorldRegexMatcher(Set<String> worldRegexes) throws PatternSyntaxException {
		for (String worldRegexStr : worldRegexes) {
			mPatterns.put(worldRegexStr, Pattern.compile(worldRegexStr));
		}

		for (World world : Bukkit.getWorlds()) {
			onLoadWorld(world);
		}
	}

	public void onLoadWorld(World world) {
		Set<String> matchingPatterns = new HashSet<>();
		mWorldPatternMatches.put(world.getName(), matchingPatterns);

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

	public void onUnloadWorld(World world) {
		mWorldPatternMatches.remove(world.getName());
	}

	// Only works for the patterns provided at matcher instantiation
	public boolean matches(World world, String worldRegex) {
		Set<String> matches = mWorldPatternMatches.get(world.getName());
		if (matches == null) {
			if (Bukkit.getWorld(world.getName()) == null) {
				// World is unloaded; no zone can contain an entity in an unloaded world
				return false;
			}
			MMLog.severe(
				"Falling back to slow regex .matches() to test unloaded world: '" + world.getName() + "' against regex: '" + worldRegex + "'",
				new IllegalStateException("WorldRegexMatcher testing against an unloaded world")
			);
			try {
				return world.getName().matches(worldRegex);
			} catch (PatternSyntaxException e) {
				MMLog.warning("Invalid world regex '" + worldRegex + "'", e);
				return false;
			}
		}
		return matches.contains(worldRegex);
	}

	public boolean matches(String worldName, String worldRegex) {
		if (worldRegex == null
				|| worldRegex.isEmpty()
				|| worldRegex.equals(".*")
				|| worldRegex.equals(worldName)
		) {
			return true;
		}

		Pattern pattern = mPatterns.get(worldRegex);
		if (pattern == null) {
			return false;
		}

		return pattern.matcher(worldName).matches();
	}
}
