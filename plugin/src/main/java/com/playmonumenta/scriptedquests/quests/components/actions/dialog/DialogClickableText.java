package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import java.util.*;

import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionBase;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionNested;
import com.playmonumenta.scriptedquests.quests.components.actions.quest.ActionQuest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;

public class DialogClickableText implements DialogBase, ActionNested {
	private final ArrayList<DialogClickableTextEntry> mEntries = new ArrayList<>();
	private final ActionNested mParent;
	public DialogClickableText(String npcName, String displayName,
	                    EntityType entityType, JsonElement element, ActionNested parent) throws Exception {
		mParent = parent;
		/*
		 * Integer used to determine which of the available clickable entries was
		 * clicked when a player clicks a chat message
		 * Choosing a random 32-bit starting number is good enough to prevent collisions
		 * Doesn't actually matter from a security perspective if collisions do happen,
		 * it'd just mean you could click dialog from a different NPC way up in chat
		 * and it'd potentially trigger a current conversation
		 */
		int entryIdx = new Random().nextInt();

		if (element.isJsonObject()) {
			mEntries.add(new DialogClickableTextEntry(npcName, displayName, entityType, element, entryIdx, parent));
		} else if (element.isJsonArray()) {
			Iterator<JsonElement> iter = element.getAsJsonArray().iterator();
			while (iter.hasNext()) {
				mEntries.add(new DialogClickableTextEntry(npcName, displayName, entityType, iter.next(), entryIdx, parent));

				entryIdx++;
			}
		} else {
			throw new Exception("clickable_text value is neither an object nor an array!");
		}
	}

	public List<DialogClickableTextEntry> getEntries() {
		return mEntries;
	}

	@Override
	public void sendDialog(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		for (DialogClickableTextEntry entry : mEntries) {
			entry.sendDialog(plugin, player, npcEntity, prereqs);
		}
	}

	@Override
	public ActionNested getParent() {
		return mParent;
	}

	@Override
	public QuestPrerequisites getPrerequisites() {
		return null;
	}

	@Override
	public List<ActionQuest> getQuestActions() {
		return new ArrayList<>();
	}

	@Override
	public List<QuestComponent> getQuestComponents(Entity entity) {
		return Collections.emptyList();
	}
}

