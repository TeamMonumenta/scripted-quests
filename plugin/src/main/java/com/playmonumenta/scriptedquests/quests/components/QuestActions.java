package com.playmonumenta.scriptedquests.quests.components;

import java.util.*;
import java.util.Map.Entry;

import com.playmonumenta.scriptedquests.quests.components.actions.*;
import com.playmonumenta.scriptedquests.quests.components.actions.quest.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;

public class QuestActions implements ActionNested {
	private final ArrayList<ActionBase> mActions = new ArrayList<>();

	// A list of quest actions that are related to quest progression.
	private List<ActionQuest> mQuestActions = new ArrayList<>();
	private QuestPrerequisites mPrerequisites = null;
	private ActionNested mParent;
	private int mDelayTicks;

	public QuestActions(String npcName, String displayName, EntityType entityType,
						int delayTicks, JsonElement element) throws Exception {
		this(npcName, displayName, entityType, delayTicks, element, null);
	}

	public QuestActions(String npcName, String displayName, EntityType entityType,
	                    int delayTicks, JsonElement element, ActionNested parent) throws Exception {
		mParent = parent;
		mDelayTicks = delayTicks;

		JsonArray array = element.getAsJsonArray();
		if (array == null) {
			throw new Exception("actions value is not an array!");
		}

		// Add all array entries
		for (JsonElement jsonElement : array) {
			JsonObject object = jsonElement.getAsJsonObject();
			if (object == null) {
				throw new Exception("actions value is not an object!");
			}

			// Add all actions in each entry object
			Set<Entry<String, JsonElement>> entries = object.entrySet();
			for (Entry<String, JsonElement> ent : entries) {
				String key = ent.getKey();

				// All action entries are single JSON things that should be passed
				// to their respective handlers
				JsonElement value = object.get(key);
				if (value == null) {
					throw new Exception("actions value for key '" + key + "' is not parseable!");
				}

				switch (key) {
					case "prerequisites":
						mPrerequisites = new QuestPrerequisites(value);
						break;
					case "command":
						mActions.add(new ActionCommand(value));
						break;
					case "command_world":
						mActions.add(new ActionCommandWorld(value));
						break;
					case "dialog":
						mActions.add(new ActionDialog(npcName, displayName, entityType, value, this));
						break;
					case "function":
						mActions.add(new ActionFunction(value));
						break;
					case "give_loot":
						mActions.add(new ActionGiveLoot(value));
						break;
					case "give_reward":
						mActions.add(new ActionGiveReward(npcName, displayName, entityType, value));
						break;
					case "interact_npc":
						mActions.add(new ActionInteractNpc(value));
						break;
					case "set_scores":
						JsonObject scoreObject = value.getAsJsonObject();
						if (scoreObject == null) {
							throw new Exception("set_scores value is not an object!");
						}

						Set<Entry<String, JsonElement>> scoreEntries = scoreObject.entrySet();
						for (Entry<String, JsonElement> scoreEnt : scoreEntries) {
							mActions.add(new ActionSetScore(scoreEnt.getKey(), scoreEnt.getValue()));
						}
						break;
					case "voice_over":
						mActions.add(new ActionVoiceOver(entityType, npcName, value));
						break;
					case "trade":
						mActions.add(new ActionTrade(value));
						break;
					case "menu":
						mActions.add(new ActionMenu(value));
						break;
					case "timer_cooldown":
						mActions.add(new ActionTimerCooldown(value));
						break;
					case "timer_reset":
						mActions.add(new ActionTimerReset(value));
						break;
					case "open_altar":
						mActions.add(new ActionOpenAltar(value));
						break;
					case "open_profession":
						mActions.add(new ActionOpenProfession(value));
						break;
					case "learn_profession":
						mActions.add(new ActionLearnProfession(value));
						break;
					case "run_actions":
						mActions.add(new ActionRunActions(value));
						break;

					case "enter_shard":
						mActions.add(new ActionEnterShard(value));
						break;
					case "leave_shard":
						mActions.add(new ActionLeaveShard(value));
						break;
					case "give_xp":
						mActions.add(new ActionGiveXP(value));
						break;
					case "give_xp_percent":
						mActions.add(new ActionGiveXPPercent(value));
						break;
					case "give_ability":
						mActions.add(new ActionGiveAbility(value));
						break;
					case "give_handbook_entry":
						mActions.add(new ActionGiveHandbookEntry(value));
						break;

					case "give_handbook_entries":
						mActions.add(new ActionGiveHandbookEntries(value));
						break;

					case "play_cutscene":
						mActions.add(new ActionPlayCutscene(npcName, displayName, entityType, value, this));
						break;

					case "rerun_components":
						if (entityType != null) {
							mActions.add(new ActionRerunComponents(npcName, entityType));
						}
						break;

					case "set_questdata":
						JsonArray questArray = value.getAsJsonArray();

						for (JsonElement questElement : questArray) {
							JsonObject questObject = questElement.getAsJsonObject();

							if (questObject == null) {
								throw new Exception("set_questdata questObject value is not an object!");
							}
							mActions.add(new ActionSetQuestData(questObject));
						}
						break;
					case "set_shard_field":
						mActions.add(new ActionSetShardData(value));
						break;
					case "set_shard_objective":
						mActions.add(new ActionSetShardObjective(value));
						break;
					case "set_shard_music":
						mActions.add(new ActionSetShardMusic(value));
						break;
					case "play_shard_title":
						mActions.add(new ActionPlayDungeonTitle(value));
						break;
					case "remove_questdata":
						mActions.add(new ActionRemoveQuestData(value));
						break;
					case "drop_loot":
						mActions.add(new ActionDropLoot(value));
						break;
					case "spawn_mob":
						JsonArray mobArray = value.getAsJsonArray();

						for (JsonElement mobElement : mobArray) {
							JsonObject mobObject = mobElement.getAsJsonObject();

							if (mobObject == null) {
								throw new Exception("spawn_mob mobObject value is not an object!");
							}
							mActions.add(new ActionSpawnMob(mobObject));
						}
						break;

					case "quest_accept":
						ActionQuestAccept actionQuestAccept = new ActionQuestAccept(npcName, displayName, value, this);
						mActions.add(actionQuestAccept);
						mQuestActions.add(actionQuestAccept);
						break;

					case "quest_complete":
						ActionQuestComplete actionQuestComplete = new ActionQuestComplete(npcName, displayName, value, this);
						mActions.add(actionQuestComplete);
						mQuestActions.add(actionQuestComplete);
						break;

					case "quest_progress":
						ActionQuestProgress actionQuestProgress = new ActionQuestProgress(value, this);
						mActions.add(actionQuestProgress);
						mQuestActions.add(actionQuestProgress);
						break;

					case "quest_reset_stage":
						ActionQuestResetStage actionQuestResetStage = new ActionQuestResetStage(value, this);
						mActions.add(actionQuestResetStage);
						mQuestActions.add(actionQuestResetStage);
						break;
					default:
						throw new Exception("Unknown actions key: " + key);
				}
			}
		}
	}

	public List<ActionBase> getActionBases() {
		return mActions;
	}

	@Override
	public ActionNested getParent() {
		return mParent;
	}

	@Override
	public QuestPrerequisites getPrerequisites() {
		return mPrerequisites;
	}

	@Override
	public List<ActionQuest> getQuestActions() {
		return mQuestActions;
	}

	@Override
	public List<QuestComponent> getQuestComponents(Entity entity) {
		return Collections.emptyList();
	}

	/**
	 * Gets the Actions from this QuestActions object.
	 * @param deep If true, it will also get the list of actions from the actions that also have a list of them.
	 * @param player The player to check all of the prerequisites for certain actions. Null if the check is not required
	 * @return A list of all the actions
	 */
	public List<ActionBase> getActions(boolean deep, Player player) {
		List<ActionBase> actions = new ArrayList<>();

		for (ActionBase action : mActions) {
			actions.add(action);
			if (deep) {

			}
		}

		return actions;
	}

	public void doActions(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		if (mPrerequisites == null || mPrerequisites.prerequisiteMet(player, npcEntity)) {
			if (mDelayTicks <= 0) {
				// If not delayed, actions can run without restrictions
				for (ActionBase action : mActions) {
					action.doAction(plugin, player, npcEntity, prereqs);
				}
			} else {
				player.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
					for (ActionBase action : mActions) {
						action.doAction(plugin, player, npcEntity, prereqs);
					}
				}, mDelayTicks);
			}
		}
	}
}
