package com.playmonumenta.scriptedquests.leaderboards;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class LeaderboardConfig {
	private final String mObjective;
	private final String mPlainDisplayName;
	private final Component mDisplayName;
	private final @Nullable String mCategory;
	private final @Nullable String mRelease;

	public LeaderboardConfig(JsonObject object) throws Exception {
		mObjective = getOrThrow("objective", object, JsonElement::getAsString);
		mPlainDisplayName = getOrThrow("plain_display_name", object, JsonElement::getAsString);
		mDisplayName = getOrThrow("display_name", object, this::parseDisplayName);
		mCategory = getOrNull("category", object, JsonElement::getAsString);
		mRelease = getOrNull("release", object, JsonElement::getAsString);
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

	public @Nullable String getRelease() {
		return mRelease;
	}

	private <T> T getOrThrow(String elementName, JsonObject object, Function<JsonElement, T> parser) throws Exception {
		JsonElement element = object.get(elementName);

		if (element == null || element.isJsonNull()) {
			throw new Exception("entry not found for element " + elementName + " in leaderboard config");
		}

		return parser.apply(element);
	}

	@Nullable
	private <T> T getOrNull(String elementName, JsonObject object, Function<JsonElement, T> parser) {
		JsonElement element = object.get(elementName);
		return (element == null || element.isJsonNull())
			? null
			: parser.apply(element);
	}

	private Component parseDisplayName(JsonElement element) {
		return element.isJsonObject()
			? MessagingUtils.GSON_COMPONENT_SERIALIZER.deserializeFromTree(element)
			: Component.text(element.getAsString(), NamedTextColor.YELLOW);
	}
}
