package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import java.util.Map.Entry;
import java.util.Set;
import org.bukkit.entity.EntityType;

public class ActionInteractNpc implements ActionBase {
	private String mName;
	private EntityType mType = EntityType.VILLAGER;

	public ActionInteractNpc(JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("interact_npc value is not an object!");
		}

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (key.equals("name")) {
				try {
					mName = ent.getValue().getAsString();
				} catch (IllegalArgumentException e) {
					throw new Exception("interact_npc name is not a string!");
				}
			} else if (key.equals("entity_type")) {
				try {
					mType = EntityType.valueOf(ent.getValue().getAsString());
				} catch (IllegalArgumentException e) {
					throw new Exception("Invalid entity_type! Must exactly match one of Spigot's EntityType values.");
				}
			} else {
				throw new Exception("Unknown interact_npc key: '" + key + "'");
			}
		}
	}

	@Override
	public void doAction(QuestContext context) {
		if (!context.getPlugin().mNpcManager.interactEvent(context.withNpc(null), mName, mType, true)) {
			context.getPlugin().getLogger().severe("No interaction available for player '" + context.getPlayer().getName() +
					                                       "' and NPC '" + mName + "'");
		}
	}
}

