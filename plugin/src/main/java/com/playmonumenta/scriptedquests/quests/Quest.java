package com.playmonumenta.scriptedquests.quests;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionGiveReward;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

// TODO Add optional prerequisites for Quest Objects

public class Quest {

	private final String mId;
	private final String mDisplayName;
	private final Integer mLevel;
	private final QuestType mType;

	private final QuestPrerequisites mPrerequisites;
	private final ActionGiveReward mRewardAction;
	private final List<QuestStage> mStages = new ArrayList<>();

	private QuestLine.QuestGroup mQuestlineGroup;
	public Quest(JsonObject object, boolean legacy) throws Exception
	{

		mId = object.get("quest_id").getAsString();
		if (mId == null) {
			throw new Exception("quest_id value is not a string!");
		}

		mDisplayName = object.get("quest_name").getAsString();
		if (mDisplayName == null) {
			throw new Exception("quest_name value is not a string!");
		}

		mLevel = object.get("level").getAsInt();

		if (!legacy) {
			mType = QuestType.valueOf(object.get("type").getAsString());

			mPrerequisites = new QuestPrerequisites(object.get("prerequisites"));
			mRewardAction = new ActionGiveReward("", "", EntityType.VILLAGER, object.get("rewards"));

			JsonArray stages = object.getAsJsonArray("quest_stages");
			for (JsonElement stage : stages) {
				mStages.add(new QuestStage(stage.getAsJsonObject()));
			}
		} else {
			mType = QuestType.SIDE;
			mRewardAction = null;
			mPrerequisites = null;

			JsonArray fields = object.get("quest_fields").getAsJsonArray();
			if (fields == null) {
				throw new Exception("quest_fields value is not an array!");
			}

			mStages.add(new QuestStage(fields));
		}

	}

	public String getQuestId() {
		return mId;
	}

	public String getDisplayName() {
		return mDisplayName;
	}

	public int getLevel() {
		return mLevel;
	}

	public QuestPrerequisites getPrerequisites() {
		return mPrerequisites;
	}

	public QuestType getType() {
		return mType;
	}

	public ActionGiveReward getRewardAction() {
		return mRewardAction;
	}

	public List<QuestStage> getStages() {
		return mStages;
	}

	public void setQuestlineGroup(QuestLine.QuestGroup group) {
		mQuestlineGroup = group;
	}

	public QuestLine.QuestGroup getQuestlineGroup() {
		return mQuestlineGroup;
	}

	public QuestStage getStage(int stageNumber) {
		int stageIndex = stageNumber - 1;
		if (stageIndex < 0) {
			return mStages.get(0);
		} else if (stageIndex >= mStages.size()) {
			return mStages.get(mStages.size() - 1);
		} else {
			return mStages.get(stageIndex);
		}
	}

	public JsonObject getNewQuestData() {
		JsonObject json = new JsonObject();

		json.addProperty("quest_id", mId);
		json.addProperty("completed", false);
		json.addProperty("stage", 1);

		json.add("stages", new JsonArray());
		return json;
	}

}
