package com.playmonumenta.scriptedquests.races;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.Nullable;

public class RaceTime implements Comparable<RaceTime> {
	private final String mLabel;
	protected final int mTime;
	private final NamedTextColor mTextColor;
	private final boolean mBold;
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
			Component colorComp = LegacyComponentSerializer.legacyAmpersand().deserialize(colorElement.getAsString());
			NamedTextColor found = findColor(colorComp);
			mTextColor = found != null ? found : NamedTextColor.WHITE;
			mBold = findBold(colorComp);
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

	public NamedTextColor getTextColor() {
		return mTextColor;
	}

	public boolean isBold() {
		return mBold;
	}

	@Override
	public int compareTo(RaceTime other) {
		// compareTo should return < 0 if this is supposed to be
		// less than other, > 0 if this is supposed to be greater than
		// other and 0 if they are supposed to be equal
		return mTime < other.mTime ? -1 : mTime == other.mTime ? 0 : 1;
	}

	private static @Nullable NamedTextColor findColor(Component component) {
		TextColor color = component.color();
		if (color instanceof NamedTextColor ntc) {
			return ntc;
		}
		for (Component child : component.children()) {
			NamedTextColor found = findColor(child);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	private static boolean findBold(Component component) {
		if (component.style().decoration(TextDecoration.BOLD) == TextDecoration.State.TRUE) {
			return true;
		}
		for (Component child : component.children()) {
			if (findBold(child)) {
				return true;
			}
		}
		return false;
	}
}
