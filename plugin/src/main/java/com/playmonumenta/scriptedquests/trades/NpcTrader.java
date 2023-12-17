package com.playmonumenta.scriptedquests.trades;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.QuestNpc;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.utils.JsonUtils;
import java.io.File;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import org.jetbrains.annotations.Nullable;

/*
 * An NpcTrader object holds prerequisites for each trade slot in the NPC's inventory
 */
public class NpcTrader {
	private final File mFile;
	private final NavigableMap<Integer, NpcTrade> mTrades = new TreeMap<>();
	private final String mOriginalNpcName;
	private final String mNpcName;
	private final @Nullable JsonElement mFilePrerequisitesJson;
	private final @Nullable QuestPrerequisites mFilePrerequisites;

	public NpcTrader(JsonObject object, File file) throws Exception {
		mFile = file;
		// Read the npc's name first
		mOriginalNpcName = JsonUtils.getString(object, "npc");
		mNpcName = QuestNpc.squashNpcName(mOriginalNpcName);

		mFilePrerequisitesJson = object.get("file_prerequisites");
		if (mFilePrerequisitesJson != null) {
			mFilePrerequisites = new QuestPrerequisites(mFilePrerequisitesJson);
		} else {
			mFilePrerequisites = null;
		}

		// Read the npc's trades
		JsonArray array = JsonUtils.getJsonArray(object, "trades");
		for (JsonElement entry : array) {
			NpcTrade trade = new NpcTrade(entry);
			if (mTrades.containsKey(trade.getIndex())) {
				// The same index is used twice, throw an error
				throw new Exception("Trader '" + mNpcName + "' specifies index " + trade.getIndex() + " more than once");
			}
			mTrades.put(trade.getIndex(), trade);
		}

		// Iterate through the remaining keys and throw an error if any are found
		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("npc") && !key.equals("trades") && !key.equals("file_prerequisites")) {
				throw new Exception("Unknown NpcTrader key: " + key);
			}
		}
	}

	public JsonObject toJson() {
		return new JsonObjectBuilder()
			       .add("npc", mOriginalNpcName)
			       .add("file_prerequisites", mFilePrerequisitesJson)
			       .add("trades", JsonUtils.toJsonArray(mTrades.values().stream().toList(), NpcTrade::toJson))
			       .build();
	}

	public File getFile() {
		return mFile;
	}

	/**
	 * NOTE: This is always the squashed/stripped version of the name! use {@link #getOriginalNpcName()} to get the full name.
	 */
	public String getNpcName() {
		return mNpcName;
	}

	public String getOriginalNpcName() {
		return mOriginalNpcName;
	}

	public @Nullable NpcTrade getTrade(int index) {
		return mTrades.get(index);
	}

	public Collection<NpcTrade> getTrades() {
		return mTrades.values();
	}

	public boolean areFilePrerequisitesMet(QuestContext context) {
		return mFilePrerequisites == null || mFilePrerequisites.prerequisiteMet(context);
	}

}
