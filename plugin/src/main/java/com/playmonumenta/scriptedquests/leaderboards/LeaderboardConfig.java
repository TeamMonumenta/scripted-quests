package com.playmonumenta.scriptedquests.leaderboards;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.utils.JsonUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

public class LeaderboardConfig {
	private final String mObjective;
	private final String mPlainDisplayName;
	private final Component mDisplayName;
	private final @Nullable String mCategory;
	private final boolean mHidden;

	public LeaderboardConfig(JsonObject object) throws Exception {
		mObjective = JsonUtils.getString(object, "objective");
		mPlainDisplayName = JsonUtils.getString(object, "plain_display_name");
		mDisplayName = parseDisplayName(object);
		mCategory = JsonUtils.getString(object, "category", null);
		mHidden = JsonUtils.getBoolean(object, "hidden", false);
	}

	public String getObjective() {
		return mObjective;
	}

	public String getPlainDisplayName() {
		return mPlainDisplayName;
	}

	public Component getDisplayName() {
		return mDisplayName;
	}

	public @Nullable String getCategory() {
		return mCategory;
	}

	public boolean isHidden() {
		return mHidden;
	}

	private Component parseDisplayName(JsonObject object) throws Exception {
		JsonElement element = object.get("display_name");

		if (element == null || element.isJsonNull()) {
			throw new Exception("entry not found for display_name in leaderboard config");
		}

		return element.isJsonObject()
			? MessagingUtils.GSON_COMPONENT_SERIALIZER.deserializeFromTree(element)
			: Component.text(element.getAsString(), NamedTextColor.YELLOW);
	}
}
