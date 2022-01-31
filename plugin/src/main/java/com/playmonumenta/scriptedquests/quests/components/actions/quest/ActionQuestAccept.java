package com.playmonumenta.scriptedquests.quests.components.actions.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.Quest;
import com.playmonumenta.scriptedquests.quests.QuestData;
import com.playmonumenta.scriptedquests.quests.QuestStage;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionBase;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionNested;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.quests.PrerequisiteCanAcceptQuest;
import me.Novalescent.Core;
import me.Novalescent.player.PlayerData;
import me.Novalescent.player.quests.QuestTemplate;
import me.Novalescent.player.scoreboards.PlayerScoreboard;
import me.Novalescent.utils.FormattedMessage;
import me.Novalescent.utils.MessageFormat;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class ActionQuestAccept extends ActionQuest implements ActionBase {

	private QuestActions mActions;
	public ActionQuestAccept(String npcName, String displayName, JsonElement value, ActionNested parent) {
		JsonObject object = value.getAsJsonObject();
		mQuestId = object.get("quest_id").getAsString();
		try {
			mActions = new QuestActions(npcName, displayName, EntityType.VILLAGER, 0, object.get("actions"), parent);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Get all combined prerequisites to get to this action.
		mPrerequisites = mergePrerequisites(parent);
		addToTopParent(parent);
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		PrerequisiteCanAcceptQuest prerequisite = new PrerequisiteCanAcceptQuest(mQuestId);

		if (prerequisite.prerequisiteMet(player, npcEntity)) {
			PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());
			Quest quest = plugin.mQuestManager.getQuest(mQuestId);
			QuestData questData = new QuestData(quest);
			questData.nextStage(quest, false);
			data.getQuestDataList().add(questData);

			player.sendTitle(ChatColor.of("#FFD05A") + "Quest Accepted",
				ChatColor.of("#FEF2C1") + quest.getDisplayName(), 10, 20 * 2, 20);
			player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2, 1.5f);
			player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1.2f);
			FormattedMessage.sendMessage(player, MessageFormat.QUESTS, "Quest Accepted: " + quest.getDisplayName());
			QuestStage stage = quest.getStage(questData.getStage());
			stage.messageObjectives(player);

			if (mActions != null) {
				mActions.doActions(plugin, player, npcEntity, prereqs);
			}
		}
	}

}
