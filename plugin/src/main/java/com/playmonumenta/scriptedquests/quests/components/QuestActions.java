package com.playmonumenta.scriptedquests.quests.components;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.playmonumenta.scriptedquests.quests.components.actions.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;

public class QuestActions {
	private ArrayList<ActionBase> mActions = new ArrayList<ActionBase>();
	private int mDelayTicks = 0;

	public QuestActions(String npcName, String displayName, EntityType entityType,
	                    int delayTicks, JsonElement element) throws Exception {
		mDelayTicks = delayTicks;

		JsonArray array = element.getAsJsonArray();
		if (array == null) {
			throw new Exception("actions value is not an array!");
		}

		// Add all array entries
		Iterator<JsonElement> iter = array.iterator();
		while (iter.hasNext()) {
			JsonObject object = iter.next().getAsJsonObject();
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
				case "command":
					mActions.add(new ActionCommand(value));
					break;
				case "dialog":
					mActions.add(new ActionDialog(npcName, displayName, entityType, value));
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
				case "open_altar":
					mActions.add(new ActionOpenAltar(value));
					break;

				case "run_actions":
					mActions.add(new ActionRunActions(value));
					break;

				case "give_xp":
					mActions.add(new ActionGiveXP(value));
					break;
				case "give_ability":
					mActions.add(new ActionGiveAbility(value));
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
				default:
					throw new Exception("Unknown actions key: " + key);
				}
			}
		}
	}

	public List<ActionBase> getActionBases() {
		return mActions;
	}

	public void doActions(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		if (mDelayTicks <= 0) {
			// If not delayed, actions can run without restrictions
			for (ActionBase action : mActions) {
				action.doAction(plugin, player, npcEntity, prereqs);
			}
		} else {
			player.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				@Override
				public void run() {
					for (ActionBase action : mActions) {
						action.doAction(plugin, player, npcEntity, prereqs);
					}
				}
			}, mDelayTicks);
		}
	}
}
