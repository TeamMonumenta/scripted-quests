package com.playmonumenta.scriptedquests.quests.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.playmonumenta.scriptedquests.quests.components.actions.ActionBase;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionNested;
import com.playmonumenta.scriptedquests.quests.components.actions.quest.ActionQuest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;

public class QuestComponent implements ActionNested {
	private ActionNested mParent;
	private QuestPrerequisites mPrerequisites = null;
	private QuestActions mActions = null;
	private List<ActionQuest> mQuestActions = new ArrayList<>();

	public QuestComponent(String npcName, String displayName,
	                      EntityType entityType, JsonElement element) throws Exception {
		this(npcName, displayName, entityType, element, null);
	}

	public QuestComponent(String npcName, String displayName,
						  EntityType entityType, JsonElement element, ActionNested parent) throws Exception {
		mParent = parent;
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("quest_components value is not an object!");
		}

		// Read the delay_actions_by_ticks field first, if specified
		JsonElement delayElement = object.get("delay_actions_by_ticks");
		int delayTicks = 0;
		if (delayElement != null) {
			delayTicks = delayElement.getAsInt();
		}

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("prerequisites") && !key.equals("actions")
				&& !key.equals("delay_actions_by_ticks")) {
				throw new Exception("Unknown quest_components key: " + key);
			}

			// All quest_components entries are single JSON things that should be passed
			// to their respective handlers
			JsonElement value = object.get(key);
			if (value == null) {
				throw new Exception("quest_components value for key '" + key + "' is not parseable!");
			}

			if (key.equals("prerequisites")) {
				mPrerequisites = new QuestPrerequisites(value);
			} else if (key.equals("actions")) {
				mActions = new QuestActions(npcName, displayName, entityType, delayTicks, value, this);
			}
		}

		if (mActions == null) {
			throw new Exception("quest_components value without an action!");
		}
	}

	@Override
	public ActionNested getParent() {
		return mParent;
	}

	@Override
	public QuestPrerequisites getPrerequisites() { return mPrerequisites; }

	@Override
	public List<ActionQuest> getQuestActions() {
		return mQuestActions;
	}

	@Override
	public List<QuestComponent> getQuestComponents(Entity entity) {
		return Collections.emptyList();
	}

	public boolean doActionsIfPrereqsMet(Plugin plugin, Player player, Entity npcEntity) {
		if (mPrerequisites == null || mPrerequisites.prerequisiteMet(player, npcEntity)) {
			mActions.doActions(plugin, player, npcEntity, mPrerequisites);
			return true;
		}
		return false;
	}

	public QuestActions getActions() {
		return mActions;
	}
}
