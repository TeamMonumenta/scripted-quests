package com.playmonumenta.scriptedquests.quests;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import com.playmonumenta.scriptedquests.quests.components.QuestComponentList;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import java.util.List;
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
	private final QuestComponentList mComponents = new QuestComponentList();
	private final String mNpcName;
	private final String mDisplayName;
	private final @Nullable QuestPrerequisites mFilePrerequisites;
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

		JsonElement prerequisites = object.get("file_prerequisites");
		if (prerequisites != null) {
			mFilePrerequisites = new QuestPrerequisites(prerequisites);
		} else {
			mFilePrerequisites = null;
		}

		// Read the npc's entity_type
		// Default to villager
		JsonElement entityType = object.get("entity_type");
		if (entityType == null) {
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

			if (!key.equals("npc") && !key.equals("display_name") && !key.equals("file_prerequisites")
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

	public List<QuestComponent> getComponents() {
		return mComponents.getComponents();
	}

	public EntityType getEntityType() {
		return mEntityType;
	}

	// Returns true if any quest components were attempted with this NPC
	// False otherwise
	public boolean interactEvent(QuestContext context, String npcName, EntityType entityType) {
		if (mEntityType.equals(entityType) && mNpcName.equals(npcName) && areFilePrerequisitesMet(context) && isVisibleToPlayer(context)) {
			// TODO this way of implementing the client protocol breaks any actions that aren't plain dialogue.
			// The actions should instead run through as normal, with any dialogue/clickable action sending a packet (or appending to a packet) instead of sending a chat message.
			// if (ClientChatProtocol.shouldSend(context.getPlayer())) {
			//     ClientChatProtocol.sendPacket(mComponents.getComponents(), context);
			// } else {
			mComponents.run(context);
			// }
			return true;
		}
		return false;
	}

	public static String squashNpcName(String name) {
		return ChatColor.stripColor(name).replaceAll("[^a-zA-Z0-9-]", "");
	}

	public boolean areFilePrerequisitesMet(QuestContext context) {
		return mFilePrerequisites == null || mFilePrerequisites.prerequisiteMet(context);
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
