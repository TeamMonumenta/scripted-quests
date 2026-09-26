package com.playmonumenta.scriptedquests.leaderboards;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

public class LeaderboardConfig {
	private final String mObjective;
	private final String mDisplayName;
	private final @Nullable String mCategory;
	private final @Nullable String mRelease;

	public LeaderboardConfig(JsonObject object) throws Exception {
		mObjective = getOrThrow("objective", object);
		mDisplayName = getOrThrow("display_name", object);
		mCategory = getOrNull("category", object);
		mRelease = getOrNull("release", object);
	}

	public String getObjective() {
		return mObjective;
	}

	public String getDisplayName() {
		return mDisplayName;
	}

	public @Nullable String getCategory() {
		return mCategory;
	}

	public @Nullable String getRelease() {
		return mRelease;
	}

	private String getOrThrow(String elementName, JsonObject object) throws Exception {
		JsonElement element = object.get(elementName);

		if (element == null
			|| !element.isJsonPrimitive()
			|| element.getAsString().isEmpty()) {
			throw new Exception("entry not found for element " + elementName + " in leaderboard config");
		}

		return element.getAsString();
	}

	@Nullable
	private String getOrNull(String elementName, JsonObject object) {
		JsonElement element = object.get(elementName);
		return (element == null
			|| !element.isJsonPrimitive())
			? null
			: element.getAsString();
	}

}
