package com.playmonumenta.scriptedquests.trades;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.QuestNpc;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/*
 * An NpcTrader object holds prerequisites for each trade slot in the NPC's inventory
 */
public class NpcTrader {
	private final HashMap<Integer, NpcTrade> mTrades = new HashMap<>();
	private final String mNpcName;
	private final @Nullable QuestPrerequisites mFilePrerequisites;

	public NpcTrader(JsonObject object) throws Exception {
		// Read the npc's name first
		JsonElement npc = object.get("npc");
		if (npc == null) {
			throw new Exception("'npc' entry is required");
		}
		if (npc.getAsString() == null || QuestNpc.squashNpcName(npc.getAsString()).isEmpty()) {
			throw new Exception("Failed to parse 'npc' name as string");
		}
		mNpcName = QuestNpc.squashNpcName(npc.getAsString());

		JsonElement prerequisites = object.get("file_prerequisites");
		if (prerequisites != null) {
			mFilePrerequisites = new QuestPrerequisites(prerequisites);
		} else {
			mFilePrerequisites = null;
		}

		// Read the npc's trades
		JsonArray array = object.getAsJsonArray("trades");
		if (array == null) {
			throw new Exception("Failed to parse 'trades' as JSON array");
		}
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

	/*
	 * NOTE: This is always the squashed/stripped version of the name!
	 */
	public String getNpcName() {
		return mNpcName;
	}

	public @Nullable NpcTrade getTrade(int index) {
		return mTrades.get(index);
	}

	public boolean areFilePrerequisitesMet(QuestContext context) {
		return mFilePrerequisites == null || mFilePrerequisites.prerequisiteMet(context);
	}

}
