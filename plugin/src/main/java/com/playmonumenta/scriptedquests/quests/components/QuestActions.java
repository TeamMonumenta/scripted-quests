package com.playmonumenta.scriptedquests.quests.components;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionBase;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionCommand;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionDialog;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionFunction;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionGiveLoot;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionInteractNpc;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionRerunComponents;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionSetScore;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionVoiceOver;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public class QuestActions {
	private final ArrayList<ActionsElement> mActions = new ArrayList<>();
	private int mDelayTicks;

	public QuestActions(@Nullable String npcName, @Nullable String displayName, @Nullable EntityType entityType,
	                    int delayTicks, JsonElement element) throws Exception {
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

			ActionsElement actions = new ActionsElement();
			mActions.add(actions);

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
						actions.mPrerequisites = new QuestPrerequisites(value);
						break;
					case "delay_actions_by_ticks":
						actions.mDelayTicks = value.getAsInt();
						break;
					case "command":
						actions.mActions.add(new ActionCommand(value));
						break;
					case "dialog":
						actions.mActions.add(new ActionDialog(npcName, displayName, entityType, value));
						break;
					case "function":
						actions.mActions.add(new ActionFunction(value));
						break;
					case "give_loot":
						actions.mActions.add(new ActionGiveLoot(value));
						break;
					case "interact_npc":
						actions.mActions.add(new ActionInteractNpc(value));
						break;
					case "set_scores":
						JsonObject scoreObject = value.getAsJsonObject();
						if (scoreObject == null) {
							throw new Exception("set_scores value is not an object!");
						}

						Set<Entry<String, JsonElement>> scoreEntries = scoreObject.entrySet();
						for (Entry<String, JsonElement> scoreEnt : scoreEntries) {
							actions.mActions.add(new ActionSetScore(scoreEnt.getKey(), scoreEnt.getValue()));
						}
						break;
					case "voice_over":
						if (entityType == null) {
							throw new Exception("Tried to create voiceover action but entityType is null");
						} else {
							actions.mActions.add(new ActionVoiceOver(entityType, npcName, value));
						}
						break;
					case "rerun_components":
						if (entityType != null) {
							actions.mActions.add(new ActionRerunComponents(npcName, entityType));
						}
						break;
					default:
						throw new Exception("Unknown actions key: " + key);
				}
			}
		}
	}

	public void doActions(QuestContext context) {
		if (mDelayTicks <= 0) {
			// If not delayed, actions can run without restrictions
			executeNow(context);
		} else {
			Bukkit.getScheduler().scheduleSyncDelayedTask(context.getPlugin(), () -> executeNow(context), mDelayTicks);
		}
	}

	private void executeNow(QuestContext context) {
		for (ActionsElement element : mActions) {
			if (element.mPrerequisites != null && !element.mPrerequisites.prerequisiteMet(context)) {
				return;
			}
			QuestContext elementContext = element.mPrerequisites != null ? context.withPrerequisites(element.mPrerequisites) : context;
			if (element.mDelayTicks <= 0) {
				for (ActionBase action : element.mActions) {
					action.doAction(elementContext);
				}
			} else {
				Bukkit.getScheduler().scheduleSyncDelayedTask(context.getPlugin(), () -> {
					for (ActionBase action : element.mActions) {
						action.doAction(elementContext);
					}
				}, element.mDelayTicks);
			}
		}
	}

	private static class ActionsElement {
		private @Nullable QuestPrerequisites mPrerequisites;
		private int mDelayTicks;
		private final ArrayList<ActionBase> mActions = new ArrayList<>();
	}

	public Optional<JsonElement> serializeForClientAPI(QuestContext context) {
		if (mDelayTicks <= 0) {
			JsonArray a = new JsonArray();
			mActions.stream()
				.flatMap(e -> e.mActions.stream())
				.map(v -> v.serializeForClientAPI(context))
				.forEach(a::add);
			return Optional.of(a);
		}

		return Optional.empty();
	}
}
