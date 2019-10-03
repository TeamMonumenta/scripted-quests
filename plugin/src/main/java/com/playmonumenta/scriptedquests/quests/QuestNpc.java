package com.playmonumenta.scriptedquests.quests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;

/*
 * A QuestNpc object holds all the quest components belonging to an NPC with a specific name
 * Only one QuestNpc object exists for each NPC with quest interactions
 * If multiple files/quests exist that use the same NPC, their QuestComponent's are
 * combined into a single QuestNpc
 */
public class QuestNpc {
	private final ArrayList<QuestComponent> mComponents = new ArrayList<QuestComponent>();
	private final String mNpcName;
	private final String mDisplayName;
	private final EntityType mEntityType;

	public QuestNpc(JsonObject object) throws Exception {
		// Read the npc's name first
		JsonElement npc = object.get("npc");
		if (npc == null) {
			throw new Exception("'npc' entry is required");
		}
		if (npc.getAsString() == null || squashNpcName(npc.getAsString()).isEmpty()) {
			throw new Exception("Failed to parse 'npc' name");
		}
		mNpcName = squashNpcName(npc.getAsString());

		// Read the npc's display name
		JsonElement displayName = object.get("display_name");
		if (displayName == null || displayName.getAsString() == null) {
			mDisplayName = npc.getAsString();
		} else {
			mDisplayName = displayName.getAsString();
		}

		// Read the npc's entity_type
		// Default to villager
		JsonElement entityType = object.get("entity_type");
		if (entityType == null || EntityType.valueOf(entityType.getAsString()) == null) {
			mEntityType = EntityType.VILLAGER;
		} else {
			mEntityType = EntityType.valueOf(entityType.getAsString());
		}

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("npc") && !key.equals("display_name")
				&& !key.equals("quest_components") && !key.equals("entity_type")) {
				throw new Exception("Unknown quest key: " + key);
			}

			// Note that 'npc' case was handled already - it had to be done first
			if (key.equals("quest_components")) {
				JsonArray array = object.getAsJsonArray(key);
				if (array == null) {
					throw new Exception("Failed to parse 'quest_components'");
				}

				Iterator<JsonElement> iter = array.iterator();
				while (iter.hasNext()) {
					JsonElement entry = iter.next();

					mComponents.add(new QuestComponent(mNpcName, mDisplayName, mEntityType, entry));
				}
			}
		}
	}

	/*
	 * NOTE: This is always the squashed/stripped version of the name!
	 */
	public String getNpcName() {
		return mNpcName;
	}

	public String getDisplayName() {
		return mDisplayName;
	}

	public ArrayList<QuestComponent> getComponents() {
		return mComponents;
	}

	public EntityType getEntityType() {
		return mEntityType;
	}

	// Combines another quest using the same NPC into this one
	public void addFromQuest(Plugin plugin, QuestNpc quest) {
		if (quest.getNpcName().equals(mNpcName)) {
			for (QuestComponent component : quest.getComponents()) {
				mComponents.add(component);
			}
		} else {
			plugin.getLogger().severe("Attempted to add two quests together with different NPCs!");
		}
	}

	// Returns true if any quest components were attempted with this NPC
	// False otherwise
	// Note: npcEntity might be null
	public boolean interactEvent(Plugin plugin, Player player, String npcName, EntityType entityType, Entity npcEntity) {
		if (mEntityType.equals(entityType) && mNpcName.equals(npcName)) {
			for (QuestComponent component : mComponents) {
				component.doActionsIfPrereqsMet(plugin, player, npcEntity);
			}
			return true;
		}
		return false;
	}

	public static String squashNpcName(String name) {
		return ChatColor.stripColor(name).replaceAll("[^a-zA-Z0-9-]", "");
	}
}
