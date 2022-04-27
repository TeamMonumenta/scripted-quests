package com.playmonumenta.scriptedquests.quests;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.api.ClientChatProtocol;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

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
	private @Nullable QuestPrerequisites mVisibilityPrerequisites;

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

		JsonElement visibilityPrerequisites = object.get("visibility_prerequisites");
		if (visibilityPrerequisites != null) {
			mVisibilityPrerequisites = new QuestPrerequisites(visibilityPrerequisites);
		}

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("npc") && !key.equals("display_name")
				    && !key.equals("quest_components") && !key.equals("entity_type") && !key.equals("visibility_prerequisites")) {
				throw new Exception("Unknown quest key: " + key);
			}

			// Note that 'npc' case was handled already - it had to be done first
			if (key.equals("quest_components")) {
				JsonArray array = object.getAsJsonArray(key);
				if (array == null) {
					throw new Exception("Failed to parse 'quest_components'");
				}

				for (JsonElement entry : array) {
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
			mComponents.addAll(quest.getComponents());
			mVisibilityPrerequisites = mVisibilityPrerequisites == null ? quest.mVisibilityPrerequisites : mVisibilityPrerequisites.union(quest.mVisibilityPrerequisites);
		} else {
			plugin.getLogger().severe("Attempted to add two quests together with different NPCs!");
		}
	}

	// Returns true if any quest components were attempted with this NPC
	// False otherwise
	public boolean interactEvent(QuestContext context, String npcName, EntityType entityType) {
		if (mEntityType.equals(entityType) && mNpcName.equals(npcName) && isVisibleToPlayer(context)) {
			if (ClientChatProtocol.shouldSend(context.getPlayer())) {
				ClientChatProtocol.sendPacket(mComponents, context);
			} else {
				for (QuestComponent component : mComponents) {
					component.doActionsIfPrereqsMet(context);
				}
			}
			return true;
		}
		return false;
	}

	public static String squashNpcName(String name) {
		return ChatColor.stripColor(name).replaceAll("[^a-zA-Z0-9-]", "");
	}

	public boolean hasVisibilityPrerequisites() {
		return mVisibilityPrerequisites != null;
	}

	public boolean isVisibleToPlayer(Player player, Entity npcEntity) {
		return isVisibleToPlayer(new QuestContext(Plugin.getInstance(), player, npcEntity, false, mVisibilityPrerequisites, player.getInventory().getItemInMainHand()));
	}

	public boolean isVisibleToPlayer(QuestContext context) {
		return mVisibilityPrerequisites == null || mVisibilityPrerequisites.prerequisiteMet(context);
	}

}
