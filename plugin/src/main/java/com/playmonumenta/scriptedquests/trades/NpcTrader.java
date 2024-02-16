package com.playmonumenta.scriptedquests.trades;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.QuestNpc;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.utils.JsonUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
	private final List<String> mOriginalNpcNames;
	private final List<String> mNpcNames;
	private final @Nullable JsonElement mFilePrerequisitesJson;
	private final @Nullable QuestPrerequisites mFilePrerequisites;

	public NpcTrader(JsonObject object, File file) throws Exception {
		mFile = file;

		// Read the npc's name first
		if (JsonUtils.getElement(object, "npc").isJsonArray()) {
			mOriginalNpcNames = new ArrayList<>();
			mNpcNames = new ArrayList<>();
			for (JsonElement npc : JsonUtils.getJsonArray(object, "npc")) {
				String name = npc.getAsString();
				mOriginalNpcNames.add(name);
				mNpcNames.add(QuestNpc.squashNpcName(name));
			}
		} else {
			String name = JsonUtils.getString(object, "npc");
			mOriginalNpcNames = List.of(name);
			mNpcNames = List.of(QuestNpc.squashNpcName(name));
		}

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
				throw new Exception("Trader '" + mNpcNames + "' specifies index " + trade.getIndex() + " more than once");
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
			       .add("npc", JsonUtils.toJsonArray(mOriginalNpcNames, JsonPrimitive::new))
			       .add("file_prerequisites", mFilePrerequisitesJson)
			       .add("trades", JsonUtils.toJsonArray(mTrades.values().stream().toList(), NpcTrade::toJson))
			       .build();
	}

	public File getFile() {
		return mFile;
	}

	/**
	 * NOTE: This is always the squashed/stripped version of the name! use {@link #getOriginalNpcNames()} to get the full name.
	 */
	public List<String> getNpcNames() {
		return mNpcNames;
	}

	public List<String> getOriginalNpcNames() {
		return mOriginalNpcNames;
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
