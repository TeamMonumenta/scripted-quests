package com.playmonumenta.scriptedquests.quests.components;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionBase;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionCancelEvent;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionCommand;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionDialog;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionFunction;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionGiveLoot;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionInteractNpc;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionRemoveItem;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionRerunComponents;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionSetScore;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionStop;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionVoiceOver;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public class QuestActions implements ActionBase {
	private final ArrayList<ActionsElement> mActions = new ArrayList<>();
	private final int mDelayTicks;

	public static int mSkipLevels = 0;

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
					case "actions":
						actions.mActions.add(new QuestActions(npcName, displayName, entityType, delayTicks, value));
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
					case "remove_items":
						if (value.isJsonArray()) {
							for (JsonElement arrayElement : value.getAsJsonArray()) {
								actions.mActions.add(new ActionRemoveItem(arrayElement));
							}
						} else {
							actions.mActions.add(new ActionRemoveItem(value));
						}
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
						if (entityType != null && npcName != null) {
							actions.mActions.add(new ActionRerunComponents(npcName, entityType));
						}
						break;
					case "stop":
						actions.mActions.add(new ActionStop(value.getAsInt()));
						break;
					case "cancel_event":
						actions.mActions.add(new ActionCancelEvent(value.getAsBoolean()));
						break;
					default:
						throw new Exception("Unknown actions key: " + key);
				}
			}
		}
	}

	@Override
	public void doActions(QuestContext context) {
		if (mDelayTicks <= 0) {
			// If not delayed, actions can run without restrictions
			executeNow(context);
		} else {
			Bukkit.getScheduler().scheduleSyncDelayedTask(context.getPlugin(), () -> executeNow(context), mDelayTicks);
		}
	}

	private void executeNow(QuestContext context) {
		mSkipLevels = 0;
		for (ActionsElement element : mActions) {
			if (element.mPrerequisites != null && !element.mPrerequisites.prerequisiteMet(context)) {
				continue;
			}
			QuestContext elementContext = element.mPrerequisites != null ? context.withPrerequisites(element.mPrerequisites) : context;
			if (element.mDelayTicks <= 0) {
				runElementActions(element, elementContext);
			} else {
				Bukkit.getScheduler().scheduleSyncDelayedTask(context.getPlugin(), () -> {
					runElementActions(element, elementContext);
				}, element.mDelayTicks);
			}
			if (mSkipLevels > 0) {
				mSkipLevels--;
				break;
			}
		}
	}

	private void runElementActions(ActionsElement element, QuestContext elementContext) {
		mSkipLevels = 0;
		for (ActionBase action : element.mActions) {
			action.doActions(elementContext);
			if (mSkipLevels > 0) {
				mSkipLevels--;
				break;
			}
		}
	}

	private static class ActionsElement {
		private @Nullable QuestPrerequisites mPrerequisites;
		private int mDelayTicks;
		private final ArrayList<ActionBase> mActions = new ArrayList<>();
	}

	@Override
	public JsonElement serializeForClientAPI(QuestContext context) {
		if (mDelayTicks <= 0) {
			JsonArray a = new JsonArray();
			mActions.stream()
				.flatMap(e -> e.mActions.stream())
				.map(v -> v.serializeForClientAPI(context))
				.forEach(a::add);
			return a;
		}

		return JsonNull.INSTANCE;
	}
}
