package com.playmonumenta.scriptedquests.quests.components;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.Entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteBase;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteCheckAdvancements;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteCheckPermissions;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteCheckScores;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteCheckTags;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteFacing;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteFullyHealed;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteGamemode;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteInventorySlotsEmpty;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteItemInEitherHand;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteItemInHand;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteItemInOffHand;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteItemsInInventory;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteLocation;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteSneaking;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteTestForBlock;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteUsedItem;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteZoneProperties;

public class QuestPrerequisites implements PrerequisiteBase {

	private enum Operator {
		AND, OR, NOT_AND, NOT_OR, ONLY_ONE_OF;
	}

	private final ArrayList<PrerequisiteBase> mPrerequisites = new ArrayList<>();
	private final Operator mOperator;
	private final boolean mUseNpcForPrereqs;

	/* Default to AND if no operator specified */
	public QuestPrerequisites(JsonElement element) throws Exception {
		this(element, Operator.AND, false);
	}

	public QuestPrerequisites(JsonElement element, Operator operator, boolean useNpcForPrereqs) throws Exception {
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
				mPrerequisites.add(new QuestPrerequisites(value, Operator.valueOf(key.toUpperCase(Locale.ROOT)), false));
				break;
			case "use_npc_for_prereqs":
				mPrerequisites.add(new QuestPrerequisites(value, Operator.AND, true));
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
			case "check_permissions": {
				JsonArray array = value.getAsJsonArray();
				if (array == null) {
					throw new Exception("Prerequisites value for key '" + key + "' is not an array!");
				}
				for (JsonElement jsonElement : array) {
					mPrerequisites.add(new PrerequisiteCheckPermissions(jsonElement));
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
			case "used_item": {
				JsonArray array = value.getAsJsonArray();
				if (array == null) {
					throw new Exception("Prerequisites value for key '" + key + "' is not an array!");
				}

				for (JsonElement jsonElement : array) {
					mPrerequisites.add(new PrerequisiteUsedItem(jsonElement));
				}
				break;
			}
			case "min_empty_inventory_slots":
				mPrerequisites.add(new PrerequisiteInventorySlotsEmpty(value));
				break;
			case "test_for_block":
				mPrerequisites.add(new PrerequisiteTestForBlock(value));
				break;
			case "location":
				mPrerequisites.add(new PrerequisiteLocation(value));
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
			case "facing":
				mPrerequisites.add(new PrerequisiteFacing(value));
				break;
			case "zone_properties":
				mPrerequisites.add(new PrerequisiteZoneProperties(value));
				break;
			default:
				throw new Exception("Unknown prerequisites key: '" + key + "'");
			}
		}
	}

	public QuestPrerequisites() {
		mOperator = Operator.AND;
		mUseNpcForPrereqs = false;
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
		case NOT_OR:
			for (PrerequisiteBase prerequisite : mPrerequisites) {
				if (prerequisite.prerequisiteMet(entity, npc)) {
					return false;
				}
			}
			return true;
		case OR:
			for (PrerequisiteBase prerequisite : mPrerequisites) {
				if (prerequisite.prerequisiteMet(entity, npc)) {
					return true;
				}
			}
			return false;
		case NOT_AND:
			for (PrerequisiteBase prerequisite : mPrerequisites) {
				if (!prerequisite.prerequisiteMet(entity, npc)) {
					return true;
				}
			}
			return false;
		case ONLY_ONE_OF:
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
		default: // AND
			for (PrerequisiteBase prerequisite : mPrerequisites) {
				if (!prerequisite.prerequisiteMet(entity, npc)) {
					return false;
				}
			}
			return true;
		}
	}
}
