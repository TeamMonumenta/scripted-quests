package com.playmonumenta.scriptedquests.quests;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class QuestLine {

	public static class QuestGroup {

		private final int mOrder;
		private final QuestLine mQuestLine;
		private final List<String> mQuestIds = new ArrayList<>();

		public QuestGroup(int order, QuestLine questline, JsonArray jsonArray) {
			mQuestLine = questline;
			mOrder = order;
			for (JsonElement element : jsonArray) {
				mQuestIds.add(element.getAsString());
			}
		}

		public int getOrderInLine() {
			return mOrder;
		}

		public QuestLine getQuestline() {
			return mQuestLine;
		}

		public List<String> getQuests() {
			return mQuestIds;
		}

	}

	private final String mId;
	private final String mName;
	private final List<QuestGroup> mQuestGroups = new ArrayList<>();

	public QuestLine(JsonObject object) {
		mId = object.get("id").getAsString();
		mName = object.get("name").getAsString();

		JsonArray questGroups = object.getAsJsonArray("quests");
		for (int i = 0; i < questGroups.size(); i++) {
			JsonElement element = questGroups.get(i);
			mQuestGroups.add(new QuestGroup(i, this, element.getAsJsonArray()));
		}

	}

	public String getId() {
		return mId;
	}

	public String getQuestlineName() {
		return mName;
	}

	public List<QuestGroup> getQuestGroups() {
		return mQuestGroups;
	}

}
