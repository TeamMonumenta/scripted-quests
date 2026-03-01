package com.playmonumenta.scriptedquests.races;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.Nullable;

public class RaceTime implements Comparable<RaceTime> {
	private final String mLabel;
	protected final int mTime;
	private final String mColor;
	private final @Nullable QuestActions mActions;

	public RaceTime(JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("times value is not an object!");
		}

		// label
		JsonElement labelElement = object.get("label");
		if (labelElement != null) {
			mLabel = labelElement.getAsString();
		} else {
			throw new Exception("times entry missing label value!");
		}

		// time
		JsonElement timeElement = object.get("time");
		double timeInSeconds;
		if (timeElement != null) {
			timeInSeconds = timeElement.getAsDouble();
		} else {
			throw new Exception("times entry missing time value!");
		}
		mTime = (int)(timeInSeconds * 1000);


		// color
		JsonElement colorElement = object.get("color");
		if (colorElement != null) {
			mColor = LegacyComponentSerializer.legacySection().serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(colorElement.getAsString()));
		} else {
			throw new Exception("times entry missing color value!");
		}

		// actions
		JsonElement actionsElement = object.get("actions");
		if (actionsElement != null) {
			// Actions should not use NPC dialog or rerun_components since they make no sense here
			mActions = new QuestActions(0, actionsElement);
		} else {
			mActions = null;
		}
	}

	public void doActions(QuestContext context) {
		if (mActions != null) {
			mActions.doActions(context);
		}
	}

	public String getLabel() {
		return mLabel;
	}

	public int getTime() {
		return mTime;
	}

	public String getColor() {
		return mColor;
	}

	// TODO: This is an awful translation layer until we update the schema to use NamedTextColor directly
	public NamedTextColor getTextColor() {
		for (int i = 0; i < mColor.length() - 1; i++) {
			if (mColor.charAt(i) == '§') {
				NamedTextColor color = switch (mColor.charAt(i + 1)) {
					case '0' -> NamedTextColor.BLACK;
					case '1' -> NamedTextColor.DARK_BLUE;
					case '2' -> NamedTextColor.DARK_GREEN;
					case '3' -> NamedTextColor.DARK_AQUA;
					case '4' -> NamedTextColor.DARK_RED;
					case '5' -> NamedTextColor.DARK_PURPLE;
					case '6' -> NamedTextColor.GOLD;
					case '7' -> NamedTextColor.GRAY;
					case '8' -> NamedTextColor.DARK_GRAY;
					case '9' -> NamedTextColor.BLUE;
					case 'a', 'A' -> NamedTextColor.GREEN;
					case 'b', 'B' -> NamedTextColor.AQUA;
					case 'c', 'C' -> NamedTextColor.RED;
					case 'd', 'D' -> NamedTextColor.LIGHT_PURPLE;
					case 'e', 'E' -> NamedTextColor.YELLOW;
					case 'f', 'F' -> NamedTextColor.WHITE;
					default -> null; // formatting code (§l, §k, etc.), skip
				};
				if (color != null) {
					return color;
				}
			}
		}
		return NamedTextColor.WHITE;
	}

	public boolean isBold() {
		return mColor.contains("§l") || mColor.contains("§L");
	}

	@Override
	public int compareTo(RaceTime other) {
		// compareTo should return < 0 if this is supposed to be
		// less than other, > 0 if this is supposed to be greater than
		// other and 0 if they are supposed to be equal
		return mTime < other.mTime ? -1 : mTime == other.mTime ? 0 : 1;
	}
}
