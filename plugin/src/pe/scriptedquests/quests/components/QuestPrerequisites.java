package pe.scriptedquests.quests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class QuestPrerequisites {
/* I give up trying to figure out how to make an enum...
	public enum Operator {
		OP_AND,
		OP_OR,
		OP_NOT
	}
*/
	private ArrayList<PrerequisiteBase> mPrerequisites = new ArrayList<PrerequisiteBase>();
//	private Operator mOperator;
	private String mOperator;

	QuestPrerequisites(JsonElement element, String operator) throws Exception {
		JsonObject object = element.getAsJsonObject();
		mOperator = operator;
		if (object == null) {
			throw new Exception("prerequisites value is not an object!");
		}

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();
			JsonElement value = ent.getValue();

			switch (key) {
			case "and":
				JsonArray array = value.getAsJsonArray();
				if (array == null) {
					throw new Exception("Prerequisites value for key '" + key + "' is not an array!");
				}

				Iterator<JsonElement> iter = array.iterator();
				while (iter.hasNext()) {
					mPrerequisites.add(QuestPrerequisites(iter.next(),"OP_AND"));
				}
				break;
			case "or":
				JsonArray array = value.getAsJsonArray();
				if (array == null) {
					throw new Exception("Prerequisites value for key '" + key + "' is not an array!");
				}

				Iterator<JsonElement> iter = array.iterator();
				while (iter.hasNext()) {
					mPrerequisites.add(QuestPrerequisites(iter.next(),"OP_OR"));
				}
				break;
			case "not":
				JsonArray array = value.getAsJsonArray();
				if (array == null) {
					throw new Exception("Prerequisites value for key '" + key + "' is not an array!");
				}

				Iterator<JsonElement> iter = array.iterator();
				while (iter.hasNext()) {
					mPrerequisites.add(QuestPrerequisites(iter.next(),"OP_NOT"));
				}
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
			case "check_tags":
				mPrerequisites.add(new PrerequisiteCheckTags(value));
				break;
			case "items_in_inventory":
				JsonArray array = value.getAsJsonArray();
				if (array == null) {
					throw new Exception("Prerequisites value for key '" + key + "' is not an array!");
				}

				Iterator<JsonElement> iter = array.iterator();
				while (iter.hasNext()) {
					mPrerequisites.add(new PrerequisiteItemsInInventory(iter.next()));
				}
				break;
			case "item_in_hand":
				JsonArray array = value.getAsJsonArray();
				if (array == null) {
					throw new Exception("Prerequisites value for key '" + key + "' is not an array!");
				}

				Iterator<JsonElement> iter = array.iterator();
				while (iter.hasNext()) {
					mPrerequisites.add(new PrerequisiteItemInHand(iter.next()));
				}
				break;
			case "location":
				mPrerequisites.add(new PrerequisiteLocation(value));
				break;
			default:
				throw new Exception("Unknown prerequisites key: '" + key + "'");
			}
		}
	}

	boolean prerequisitesMet(Player player) {
		switch(mOperator) {
		case "OP_OR":
			for (PrerequisiteBase prerequisite : mPrerequisites) {
				if (prerequisite.prerequisiteMet(player)) {
					return true;
				}
			}

			return false;
		case "OP_NOT":
			for (PrerequisiteBase prerequisite : mPrerequisites) {
				if (prerequisite.prerequisiteMet(player)) {
					return false;
				}
			}

			return true;
		default:
		// "OP_AND"
			for (PrerequisiteBase prerequisite : mPrerequisites) {
				if (!prerequisite.prerequisiteMet(player)) {
					return false;
				}
			}

			return true;
		}
	}
}
