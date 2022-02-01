package com.playmonumenta.scriptedquests.quests.components.actions.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.*;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionBase;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionNested;
import me.Novalescent.Core;
import me.Novalescent.player.PlayerData;
import me.Novalescent.player.options.RPGOption;
import me.Novalescent.player.quests.QuestTemplate;
import me.Novalescent.player.scoreboards.PlayerScoreboard;
import me.Novalescent.utils.FormattedMessage;
import me.Novalescent.utils.MessageFormat;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ActionQuestProgress extends ActionQuest implements ActionBase {

	private static class ProgressChange {
		private int mOperation;
		private static final int SET_EXACT = 1;
		private static final int INCREMENT = 2;

		private final int mStage;
		private final String mObjectiveId;
		private final int mValue;

		public ProgressChange(JsonObject object) {
			mStage = object.get("stage").getAsInt();
			mObjectiveId = object.get("objective_id").getAsString();
			String parameter = object.get("objective_value").getAsString();

			mOperation = SET_EXACT;
			if (parameter.startsWith("+") || parameter.startsWith("-")) {
				mOperation = INCREMENT;
				parameter = parameter.substring(1);
			}

			mValue = Integer.parseInt(parameter);
		}

		public void change(Player player, Quest quest, QuestData data) {
			if (mStage > -2) {
				int actualStage = mStage == -1 ? data.getStage() : mStage;
				QuestStage stage = quest.getStage(actualStage);

				QuestObjective objective = stage.getObjective(mObjectiveId);
				if (objective != null) {
					QuestStageData stageData = data.getStageData(actualStage);

					switch (mOperation) {
						case SET_EXACT -> stageData.setObjective(mObjectiveId, objective.getObjectiveMax(), mValue, objective.getObjectiveDefault());
						case INCREMENT -> stageData.changeObjective(mObjectiveId, objective.getObjectiveMax(), mValue, objective.getObjectiveDefault());
					}

					if (data.getStage() == actualStage && stageData.isStageCompleted(stage)) { // The stage we changed was the player current's stage, and they just completed it.
						QuestStageData newStageData = data.nextStage(quest, true);
						if (newStageData != null) {
							FormattedMessage.sendMessage(player, MessageFormat.QUESTS, ChatColor.of("#FFD05A") + quest.getDisplayName() + ChatColor.of("#FEF2C1")
							+ " has been updated.");
							QuestStage newStage = quest.getStage(data.getStage());
							newStage.messageObjectives(player);
						}
					}
				}
			}
		}

	}

	private final List<ProgressChange> mChanges = new ArrayList<>();

	public ActionQuestProgress(JsonElement value, ActionNested parent) {
		JsonObject object = value.getAsJsonObject();

		mQuestId = object.get("quest_id").getAsString();

		JsonArray progressChanges = object.get("progress_changes").getAsJsonArray();

		for (JsonElement element : progressChanges) {
			mChanges.add(new ProgressChange(element.getAsJsonObject()));
		}

		mPrerequisites = mergePrerequisites(parent);
		addToTopParent(parent);
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		// Set completion first
		PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());
		QuestData questData = data.getQuestData(mQuestId);
		if (questData != null) {
			Quest quest = plugin.mQuestManager.getQuest(mQuestId);
			if (quest != null) {
				for (ProgressChange change : mChanges) {
					change.change(player, quest, questData);
				}

				QuestData tracked = data.getTrackedQuest();
				if ((boolean) data.mOptions.getOptionValue(RPGOption.AUTOTRACK_QUESTS) &&
					!questData.mTracked && tracked == null) {
					questData.mTracked = true;
				}

				if (data.mScoreboard.mTemplate == null || data.mScoreboard.mTemplate instanceof QuestTemplate) {
					data.mScoreboard.mTemplate = new QuestTemplate();
					data.mScoreboard.updateScoreboard();
				}

				data.updateQuestVisibility();
			}
		}

	}

}
