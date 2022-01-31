package com.playmonumenta.scriptedquests.quests.components.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import com.playmonumenta.scriptedquests.quests.components.actions.dialog.*;
import com.playmonumenta.scriptedquests.quests.components.actions.quest.ActionQuest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;

public class ActionDialog implements ActionBase, ActionNested {
	private final ArrayList<DialogBase> mDialogs = new ArrayList<>();
	private final ArrayList<DialogClickableText> mClickableTexts = new ArrayList<>();
	private final ActionNested mParent;
	public ActionDialog(String npcName, String displayName,
	             EntityType entityType, JsonElement element, ActionNested parent) throws Exception {
		JsonObject object = element.getAsJsonObject();
		mParent = parent;
		if (object == null) {
			throw new Exception("dialog value is not an object!");
		}

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (key.equals("text")) {
				mDialogs.add(new DialogText(displayName, ent.getValue()));
			} else if (key.equals("raw_text")) {
				mDialogs.add(new DialogRawText(ent.getValue()));
			} else if (key.equals("clickable_text")) {
				mClickableTexts.add(new DialogClickableText(npcName, displayName, entityType, ent.getValue(), this));
			} else if (key.equals("random_text")) {
				mDialogs.add(new DialogRandomText(displayName, ent.getValue()));
			} else if (key.equals("raw_random_text")) {
				mDialogs.add(new DialogRawRandomText(ent.getValue()));
			} else if (key.equals("scrolling_text")) {
				mDialogs.add(new DialogScrollingText(displayName, ent.getValue(), this));
			}  else {
				throw new Exception("Unknown dialog key: '" + key + "'");
			}
		}
	}

	public List<DialogBase> getDialog() {
		List<DialogBase> dialogs = new ArrayList<>();
		dialogs.addAll(mDialogs);
		dialogs.addAll(mClickableTexts);
		return dialogs;
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		for (DialogBase dialog : mDialogs) {
			dialog.sendDialog(plugin, player, npcEntity, prereqs);
		}

		// Run the Clickable Dialogs AFTER everything else, because we always want clickable text to appear at the bottom
		for (DialogClickableText clickableText : mClickableTexts) {
			clickableText.sendDialog(plugin, player, npcEntity, prereqs);
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
