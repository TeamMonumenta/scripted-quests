package com.playmonumenta.scriptedquests.quests;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import com.playmonumenta.scriptedquests.quests.components.QuestComponentList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/**
 * A ClickableEntry is a container for an interaction the player can trigger at any time
 * by clicking an in-game object (sign, book entry) or by typing the command
 * /click <label>
 *
 * Labels must be globally unique
 */
public class ClickableEntry {
	private final QuestComponentList mComponents = new QuestComponentList();
	private final String mLabel;
	private final String mDisplayName;

	public ClickableEntry(JsonObject object) throws Exception {
		//////////////////////////////////////// label (Required) ////////////////////////////////////////
		JsonElement label = object.get("label");
		if (label == null) {
			throw new Exception("'label' entry is required");
		}
		if (label.getAsString() == null || squashLabel(label.getAsString()).isEmpty()) {
			throw new Exception("Failed to parse 'label' as string");
		}
		mLabel = squashLabel(label.getAsString());

		//////////////////////////////////////// display_name (Optional) ////////////////////////////////////////
		// Read the optional display name - default to empty string
		JsonElement displayName = object.get("display_name");
		if (displayName == null || displayName.getAsString() == null) {
			mDisplayName = "";
		} else {
			mDisplayName = displayName.getAsString();
		}

		//////////////////////////////////////// quest_components (Required) ////////////////////////////////////////
		JsonElement questComponents = object.get("quest_components");
		if (questComponents == null) {
			throw new Exception("'quest_components' entry is required");
		}
		JsonArray array = questComponents.getAsJsonArray();
		if (array == null) {
			throw new Exception("Failed to parse 'quest_components' as JSON array");
		}

		for (JsonElement entry : array) {
			// TODO: Refactor so that components only require a linkage to the top-level item, not a name/entity type
			mComponents.add(new QuestComponent("", mDisplayName, EntityType.PLAYER, entry));
		}

		//////////////////////////////////////// Fail if other keys exist ////////////////////////////////////////
		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("label") && !key.equals("display_name")
				    && !key.equals("quest_components")) {
				throw new Exception("Unknown quest key: " + key);
			}
		}
	}

	public String getLabel() {
		return mLabel;
	}

	public List<QuestComponent> getComponents() {
		return mComponents.getComponents();
	}

	public void clickEvent(Plugin plugin, Player player) {
		mComponents.run(new QuestContext(plugin, player, null));
	}

	public static String squashLabel(String label) {
		return ChatColor.stripColor(label).toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
	}
}
