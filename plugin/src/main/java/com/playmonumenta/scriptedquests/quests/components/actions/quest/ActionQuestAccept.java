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
import com.playmonumenta.scriptedquests.quests.components.actions.ActionGiveReward;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionNested;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.quests.PrerequisiteCanAcceptQuest;
import me.Novalescent.Constants;
import me.Novalescent.Core;
import me.Novalescent.player.ExpandingTitle;
import me.Novalescent.player.PlayerData;
import me.Novalescent.player.options.RPGOption;
import me.Novalescent.player.quests.QuestTemplate;
import me.Novalescent.player.scoreboards.PlayerScoreboard;
import me.Novalescent.utils.FormattedMessage;
import me.Novalescent.utils.MessageFormat;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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
		Quest quest = plugin.mQuestManager.getQuest(mQuestId);
		if (quest != null) {
			ActionGiveReward.RewardMenu rewardMenu = new ActionGiveReward.RewardMenu(quest.getRewardAction());
			rewardMenu.setDisplayOnly(true);
			rewardMenu.setTitle("Accept \"" + quest.getDisplayName() + "\"?");
			rewardMenu.setOKButtonText("Accept Quest");
			rewardMenu.setOKButtonLore("You will earn the rewards above upon completing this quest.");
			rewardMenu.setOnOK(() -> {
				PrerequisiteCanAcceptQuest prerequisite = new PrerequisiteCanAcceptQuest(mQuestId);

				if (prerequisite.prerequisiteMet(player, npcEntity)) {
					PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());

					QuestData questData = new QuestData(quest);
					questData.nextStage(quest, false);
					data.getQuestDataList().add(questData);

					new BukkitRunnable() {
						final ExpandingTitle acceptTitle = new ExpandingTitle("Quest Accepted", 0, Constants.QUEST_MAIN_COLOR, Constants.QUEST_SUB_COLOR);
						final ExpandingTitle nameTitle = new ExpandingTitle(quest.getDisplayName(), 12, Constants.QUEST_MAIN_COLOR, Constants.QUEST_SUB_COLOR);

						boolean retract = false;
						int maxTime = 0;
						@Override
						public void run() {
							if (!data.isLoggedIn()) {
								this.cancel();
								return;
							}

							if (!retract) {
								acceptTitle.increment();

								if (!nameTitle.increment()) {
									maxTime++;
									if (maxTime >= 50) {
										retract = true;
										player.playSound(player.getLocation(), Sound.UI_TOAST_OUT, 2, 1);
										player.playSound(player.getLocation(), Sound.UI_TOAST_OUT, 2, 1.4f);
									}
								}
								if (acceptTitle.getIncrements() == 1) {
									player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 2, 1);
								}

								if (nameTitle.getIncrements() == 13) {
									player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 2, 1.4f);
								}
							} else {
								acceptTitle.decrement();
								nameTitle.decrement();

								if (acceptTitle.getIncrements() <= 0 || nameTitle.getIncrements() <= 0) {
									this.cancel();
								}
							}

							player.sendTitle(acceptTitle.toString(),
								nameTitle.toString(), 0, 20 * 3, 0);

						}

					}.runTaskTimer(plugin, 0, 1);

					player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2, 1.5f);
					player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1.2f);
					if (mActions != null) {
						mActions.doActions(plugin, player, npcEntity, prereqs);
					}

					FormattedMessage.sendMessage(player, MessageFormat.QUESTS, "Quest Accepted: " + Constants.QUEST_MAIN_COLOR + quest.getDisplayName());
					QuestStage stage = quest.getStage(questData.getStage());
					stage.messageObjectives(player);

					QuestData tracked = data.getTrackedQuest();

					if ((boolean) data.mOptions.getOptionValue(RPGOption.AUTOTRACK_QUESTS) &&
						!questData.mTracked && tracked == null) {
						questData.mTracked = true;
						tracked = questData;
					}

					PlayerScoreboard scoreboard = data.mScoreboard;
					if (scoreboard.mTemplate != null) {
						if (scoreboard.mTemplate instanceof QuestTemplate) {
							scoreboard.updateScoreboard();
						}
					} else if (tracked != null) {
						scoreboard.mTemplate = new QuestTemplate();
						scoreboard.updateScoreboard();
					}

					data.updateQuestVisibility();

				} else {
					FormattedMessage.sendMessage(player, MessageFormat.QUESTS, ChatColor.RED
						+ "You attempted to accept a quest that you should not be able to accept! Please report this bug ASAP! Quest ID: " + getQuestId());
				}
			});

			rewardMenu.openMenu(plugin, npcEntity, player);
		}

	}

}
