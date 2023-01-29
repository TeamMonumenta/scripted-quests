package com.playmonumenta.scriptedquests.trades;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.utils.MMLog;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.entity.EntityType;

public class NpcTrade implements Comparable<NpcTrade> {

	private final int mIndex;
	private final QuestPrerequisites mPrerequisites;
	private @Nullable QuestActions mActions = null;
	private int mCount = -1;

	public NpcTrade(JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("trade value is not an object!");
		}

		// index
		JsonElement indexElement = object.get("index");
		if (indexElement != null) {
			mIndex = indexElement.getAsInt();
		} else {
			throw new Exception("trade entry missing index value!");
		}

		// prerequisites
		JsonElement prereqElement = object.get("prerequisites");
		if (prereqElement != null) {
			mPrerequisites = new QuestPrerequisites(prereqElement);
		} else {
			throw new Exception("trade entry missing mPrerequisites value!");
		}

		// actions (optional)
		JsonElement actionsElement = object.get("actions");
		if (actionsElement != null) {
			mActions = new QuestActions("", "", EntityType.VILLAGER, 0, actionsElement);
		}

		JsonElement countElement = object.get("count");
		if (countElement != null) {
			mCount = countElement.getAsInt();
		}

		// Iterate through the remaining keys and throw an error if any are found
		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("index") && !key.equals("prerequisites") && !key.equals("actions") && !key.equals("count")) {
				throw new Exception("Unknown trade key: " + key);
			}
		}
	}

	public NpcTrade(int mIndex, QuestPrerequisites mPrerequisites, TradeWindowOpenEvent.Trade trade) {
		this(mIndex, mPrerequisites, trade.getActions(), trade.getCount());
	}

	public NpcTrade(int mIndex, QuestPrerequisites mPrerequisites, @Nullable QuestActions mActions, int mCount) {
		this.mIndex = mIndex;
		this.mPrerequisites = mPrerequisites;
		this.mActions = mActions;
		this.mCount = mCount;
	}

	public int getIndex() {
		return mIndex;
	}

	public @Nullable QuestActions getActions() {
		return mActions;
	}

	public boolean prerequisiteMet(QuestContext context) {
		return mPrerequisites.prerequisiteMet(context);
	}

	public void doActions(QuestContext context) {
		if (mActions != null) {
			mActions.doActions(context);
		}
	}

	public int getCount() {
		return mCount;
	}

	@Override
	public int compareTo(NpcTrade other) {
		// compareTo should return < 0 if this is supposed to be
		// less than other, > 0 if this is supposed to be greater than
		// other and 0 if they are supposed to be equal
		return mIndex < other.mIndex ? -1 : mIndex == other.mIndex ? 0 : 1;
	}
}
