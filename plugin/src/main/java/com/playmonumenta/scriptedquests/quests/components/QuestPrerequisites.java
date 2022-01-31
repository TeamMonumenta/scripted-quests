package com.playmonumenta.scriptedquests.quests.components;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.playmonumenta.scriptedquests.quests.components.prerequisites.*;
import org.bukkit.entity.Entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class QuestPrerequisites implements PrerequisiteBase {
	private final ArrayList<PrerequisiteBase> mPrerequisites = new ArrayList<PrerequisiteBase>();
	private final String mOperator;
	private final boolean mUseNpcForPrereqs;

	public QuestPrerequisites(List<PrerequisiteBase> prerequisites) {
		mPrerequisites.addAll(prerequisites);
		mOperator = "and";
		mUseNpcForPrereqs = false;
	}

	/* Default to AND if no operator specified */
	public QuestPrerequisites(JsonElement element) throws Exception {
		this(element, "and", false);
	}

	public QuestPrerequisites(JsonElement element, String operator, boolean useNpcForPrereqs) throws Exception {
		JsonObject object = element.getAsJsonObject();
		mOperator = operator;
		mUseNpcForPrereqs = useNpcForPrereqs;

		if (object == null) {
			throw new Exception("prerequisites value is not an object!");
		}

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();
			JsonElement value = ent.getValue();

			switch (key) {
			case "and":
			case "or":
			case "not_and":
			case "not_or":
			case "only_one_of":
				mPrerequisites.add(new QuestPrerequisites(value, key, false));
				break;
			case "use_npc_for_prereqs":
				mPrerequisites.add(new QuestPrerequisites(value, "and", true));
				break;
			case "check_scores":
				JsonObject scoreObject = value.getAsJsonObject();
				if (scoreObject == null) {
					throw new Exception("check_scores value is not an object!");
				}

				Set<Entry<String, JsonElement>> scoreEntries = scoreObject.entrySet();
				for (Entry<String, JsonElement> scoreEnt : scoreEntries) {
					mPrerequisites.add(new PrerequisiteCheckScores(scoreEnt.getKey(), scoreEnt.getValue()));
				}
				break;
			case "check_advancements": {
				JsonArray array = value.getAsJsonArray();
				if (array == null) {
					throw new Exception("Prerequisites value for key '" + key + "' is not an array!");
				}

				// Add all array entries
				Iterator<JsonElement> iter = array.iterator();
				while (iter.hasNext()) {
					mPrerequisites.add(new PrerequisiteCheckAdvancements(iter.next()));
				}
				break;
			}
			case "check_tags": {
				JsonArray array = value.getAsJsonArray();
				if (array == null) {
					throw new Exception("Prerequisites value for key '" + key + "' is not an array!");
				}

				// Add all array entries
				Iterator<JsonElement> iter = array.iterator();
				while (iter.hasNext()) {
					mPrerequisites.add(new PrerequisiteCheckTags(iter.next()));
				}
				break;
			}
			case "items_in_inventory": {
				JsonArray array = value.getAsJsonArray();
				if (array == null) {
					throw new Exception("Prerequisites value for key '" + key + "' is not an array!");
				}

				Iterator<JsonElement> iter = array.iterator();
				while (iter.hasNext()) {
					mPrerequisites.add(new PrerequisiteItemsInInventory(iter.next()));
				}
				break;
			}
			case "item_in_hand": {
				JsonArray array = value.getAsJsonArray();
				if (array == null) {
					throw new Exception("Prerequisites value for key '" + key + "' is not an array!");
				}

				for (JsonElement jsonElement : array) {
					mPrerequisites.add(new PrerequisiteItemInHand(jsonElement));
				}
				break;
			}
			case "item_in_off_hand": {
				JsonArray array = value.getAsJsonArray();
				if (array == null) {
					throw new Exception("Prerequisites value for key '" + key + "' is not an array!");
				}

				for (JsonElement jsonElement : array) {
					mPrerequisites.add(new PrerequisiteItemInOffHand(jsonElement));
				}
				break;
			}
			case "item_in_either_hand": {
				JsonArray array = value.getAsJsonArray();
				if (array == null) {
					throw new Exception("Prerequisites value for key '" + key + "' is not an array!");
				}

				for (JsonElement jsonElement : array) {
					mPrerequisites.add(new PrerequisiteItemInEitherHand(jsonElement));
				}
				break;
			}
			case "test_for_block":
				mPrerequisites.add(new PrerequisiteTestForBlock(value));
				break;
			case "location":
				mPrerequisites.add(new PrerequisiteLocation(value));
				break;
			case "timer_cooldown":
				mPrerequisites.add(new PrerequisiteTimerCooldown(value));
				break;
			case "is_fully_healed":
				mPrerequisites.add(new PrerequisiteFullyHealed(value));
				break;
			case "is_sneaking":
				mPrerequisites.add(new PrerequisiteSneaking(value));
				break;
			case "gamemode":
				mPrerequisites.add(new PrerequisiteGamemode(value));
				break;
			case "check_questdata":
				JsonArray array = value.getAsJsonArray();
				if (array == null) {
					throw new Exception("Prerequisites value for key '" + key + "' is not an array!");
				}

				Iterator<JsonElement> iter = array.iterator();
				while (iter.hasNext()) {
					mPrerequisites.add(new PrerequisiteCheckQuestData(iter.next()));
				}
				break;

			case "check_level":
				mPrerequisites.add(new PrerequisiteCheckLevel(value));
				break;

			case "check_class":
				mPrerequisites.add(new PrerequisiteCheckClass(value));
				break;
			case "check_shard":
				mPrerequisites.add(new PrerequisiteCheckShard(value));
				break;
			default:
				throw new Exception("Unknown prerequisites key: '" + key + "'");
			}
		}
	}

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npc) {
		/*
		 * This will cause all subsequent checks to use the npc for the entity to run
		 * prereq checks against
		 */
		if (mUseNpcForPrereqs) {
			if (npc != null) {
				entity = npc;
			} else {
				/*
				 * There is no NPC to use here. This is purely an error with
				 * the quest design.
				 * TODO: Throw an error to the console log...
				 */
				return false;
			}
		}

		switch (mOperator) {
		case "not_or":
			for (PrerequisiteBase prerequisite : mPrerequisites) {
				if (prerequisite.prerequisiteMet(entity, npc)) {
					return false;
				}
			}
			return true;
		case "or":
			for (PrerequisiteBase prerequisite : mPrerequisites) {
				if (prerequisite.prerequisiteMet(entity, npc)) {
					return true;
				}
			}
			return false;
		case "not_and":
			for (PrerequisiteBase prerequisite : mPrerequisites) {
				if (!prerequisite.prerequisiteMet(entity, npc)) {
					return true;
				}
			}
			return false;
		case "only_one_of":
			boolean val = false;
			for (PrerequisiteBase prerequisite : mPrerequisites) {
				if (prerequisite.prerequisiteMet(entity, npc)) {
					if (val) {
						/*
						 * Had a true value, then another true value
						 * Definitely not only_one_of
						 */
						return false;
					} else {
						/*
						 * No true value yet - store it and keep looping
						 */
						val = true;
					}
				}
			}
			return val;
		default: // "and"
			for (PrerequisiteBase prerequisite : mPrerequisites) {
				if (!prerequisite.prerequisiteMet(entity, npc)) {
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * Returns a clone of the Prerequisites list for this QuestPrerequisites
	 * @return A clone of the Prerequisites list
	 */
	public List<PrerequisiteBase> getPrerequisites() {
		return new ArrayList<>(mPrerequisites);
	}

	/**
	 * Merges the specified QuestPrerequisites into this one.
	 * @param toMerge The QuestPrerequisites to merge with
	 * @return A cloned version of these prerequisites with the new, merged version.
	 */
	public QuestPrerequisites merge(QuestPrerequisites toMerge) {
		List<PrerequisiteBase> prerequisitesList = getPrerequisites();
		if (toMerge != null && !toMerge.getPrerequisites().isEmpty()) {
			prerequisitesList.add(toMerge);
		}
		return new QuestPrerequisites(prerequisitesList);
	}
}
