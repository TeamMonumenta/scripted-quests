package pe.scriptedquests.quests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.scriptedquests.Plugin;

class QuestActions {
	private ArrayList<ActionBase> mActions = new ArrayList<ActionBase>();
	private int mDelayTicks = 0;

	QuestActions(String npcName, String displayName, EntityType entityType,
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
					mActions.add(new ActionVoiceOver(value));
					break;
				case "rerun_components":
					mActions.add(new ActionRerunComponents(npcName, entityType));
					break;
				default:
					throw new Exception("Unknown actions key: " + key);
				}
			}
		}
	}

	void doActions(Plugin plugin, Player player, QuestPrerequisites prereqs) {
		if (mDelayTicks <= 0) {
			// If not delayed, actions can run without restrictions
			for (ActionBase action : mActions) {
				action.doAction(plugin, player, prereqs);
			}
		} else {
			player.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				@Override
				public void run() {
					for (ActionBase action : mActions) {
						action.doAction(plugin, player, prereqs);
					}
				}
			}, mDelayTicks);
		}
	}
}
