package com.playmonumenta.scriptedquests.quests.components.actions.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.*;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionBase;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionGiveReward;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionNested;
import me.Novalescent.Core;
import me.Novalescent.player.ExpandingTitle;
import me.Novalescent.player.PlayerData;
import me.Novalescent.player.quests.QuestTemplate;
import me.Novalescent.utils.FormattedMessage;
import me.Novalescent.utils.MessageFormat;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ActionQuestComplete extends ActionQuest implements ActionBase {

	private QuestActions mActions;
	public ActionQuestComplete(String npcName, String displayName, JsonElement value, ActionNested parent) {
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
			PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());
			QuestData questData = data.getQuestData(mQuestId);
			if (questData != null) {
				ActionGiveReward.RewardMenu rewardMenu = new ActionGiveReward.RewardMenu(quest.getRewardAction());
				rewardMenu.setTitle("Complete \"" + quest.getDisplayName() + "\"");
				rewardMenu.setOnOK(() -> {

					// Clear current stage data and re-add all of the stage data
					questData.getStages().clear();

					for (QuestStage stage : quest.getStages()) {
						questData.getStages().add(stage.getNewStageData(mQuestId, true));
					}
					questData.mCompleted = true;

					new BukkitRunnable() {
						final ExpandingTitle acceptTitle = new ExpandingTitle("Quest Complete", 0, ChatColor.of("#FFD05A"), ChatColor.of("#FEF2C1"));
						final ExpandingTitle nameTitle = new ExpandingTitle(quest.getDisplayName(), 12, ChatColor.of("#FFD05A"), ChatColor.of("#FEF2C1"));

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
					player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 2, 1.25f);
					player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
					if (mActions != null) {
						mActions.doActions(plugin, player, npcEntity, prereqs);
					}

					FormattedMessage.sendMessage(player, MessageFormat.QUESTS, "Quest Completed: " + ChatColor.of("#FFD05A") + quest.getDisplayName());
					if (data.mScoreboard.mTemplate instanceof QuestTemplate) {
						data.mScoreboard.updateScoreboard();
					}

					data.updateQuestVisibility();
				});
				rewardMenu.openMenu(plugin, npcEntity, player);
			}

		}

	}

}
