package com.playmonumenta.scriptedquests.trades;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.utils.JsonUtils;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

public class NpcTrade implements Comparable<NpcTrade> {

	private final int mIndex;
	private final JsonElement mPrerequisitesJson;
	private final QuestPrerequisites mPrerequisites;
	private @Nullable JsonElement mActionsJson = null;
	private @Nullable QuestActions mActions = null;
	private int mCount = -1;
	private @Nullable List<ItemStack> mOverrideTradeItems;
	private @Nullable ItemStack mOriginalResult = null;

	public NpcTrade(JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("trade value is not an object!");
		}

		// index
		mIndex = JsonUtils.getInt(object, "index");

		// prerequisites
		JsonElement prereqElement = object.get("prerequisites");
		if (prereqElement != null) {
			mPrerequisitesJson = prereqElement;
			mPrerequisites = new QuestPrerequisites(prereqElement);
		} else {
			throw new Exception("trade entry missing prerequisites value!");
		}

		// actions (optional)
		mActionsJson = object.get("actions");
		if (mActionsJson != null) {
			mActions = new QuestActions("", "", EntityType.VILLAGER, 0, mActionsJson);
		}

		JsonElement countElement = object.get("count");
		if (countElement != null) {
			mCount = countElement.getAsInt();
		}

		JsonElement overrideElement = object.get("override_items");
		if (overrideElement != null) {
			JsonArray overrides = overrideElement.getAsJsonArray();
			if (overrides.size() != 3) {
				throw new Exception("The number of items in 'override_items' must be 3 (two ingredients and one result)");
			}
			mOverrideTradeItems = new ArrayList<>(3);
			for (JsonElement override : overrides) {
				mOverrideTradeItems.add(NBTItem.convertNBTtoItem(new NBTContainer(override.getAsString())));
			}
		}


		// Iterate through the remaining keys and throw an error if any are found
		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("index") && !key.equals("prerequisites") && !key.equals("actions") && !key.equals("count") && !key.equals("override_items")) {
				throw new Exception("Unknown trade key: " + key);
			}
		}
	}

	public NpcTrade(int mIndex, QuestPrerequisites mPrerequisites, TradeWindowOpenEvent.Trade trade) {
		this(mIndex, mPrerequisites, trade.getActions(), trade.getCount(), trade.getOriginalResult());
	}

	public NpcTrade(int mIndex, QuestPrerequisites mPrerequisites, @Nullable QuestActions mActions, int mCount, @Nullable ItemStack mOriginalResult) {
		this.mIndex = mIndex;
		mPrerequisitesJson = null;
		this.mPrerequisites = mPrerequisites;
		this.mActions = mActions;
		this.mCount = mCount;
		this.mOriginalResult = mOriginalResult;
	}

	public JsonObject toJson() {
		if (mPrerequisitesJson == null || (mActionsJson == null && mActions != null) || mOriginalResult != null) {
			throw new IllegalStateException("Cannot serialize a trade that was not read from JSON");
		}
		JsonArray overrides = null;
		if (mOverrideTradeItems != null) {
			overrides = new JsonArray(3);
			for (ItemStack override : mOverrideTradeItems) {
				overrides.add(NBTItem.convertItemtoNBT(override).toString());
			}
		}
		return new JsonObjectBuilder()
			       .add("index", mIndex)
			       .add("prerequisites", mPrerequisitesJson)
			       .add("actions", mActionsJson)
			       .add("count", mCount == -1 ? null : new JsonPrimitive(mCount))
			       .add("override_items", overrides)
			       .build();
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

	public @Nullable List<ItemStack> getOverrideTradeItems() {
		return mOverrideTradeItems;
	}

	public void setOverrideTradeItems(@Nullable List<ItemStack> mOverrideTradeItems) {
		this.mOverrideTradeItems = mOverrideTradeItems;
	}

	public @Nullable ItemStack getOriginalResult() {
		return mOriginalResult;
	}

	@Override
	public int compareTo(NpcTrade other) {
		// compareTo should return < 0 if this is supposed to be
		// less than other, > 0 if this is supposed to be greater than
		// other and 0 if they are supposed to be equal
		return Integer.compare(mIndex, other.mIndex);
	}
}
