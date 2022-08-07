package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.quests.components.actions.quest.ActionQuest;
import me.Novalescent.Core;
import me.Novalescent.actorsystem.Scene;
import me.Novalescent.cutscenes.Cutscene;
import me.Novalescent.utils.Utils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActionPlayCutsceneNew implements ActionBase, ActionNested {

	private final String mCutsceneId;

	private final List<QuestComponent> mComponents = new ArrayList<>();
	private final ActionNested mParent;
	public ActionPlayCutsceneNew(String npcName, String displayName, EntityType entityType, JsonElement value, ActionNested parent) throws Exception {
		mParent = parent;
		JsonObject json = value.getAsJsonObject();

		mCutsceneId = json.get("cutscene_id").getAsString();
		if (mCutsceneId == null) {
			throw new Exception("cutscene_id is an invalid string!");
		}

		for (JsonElement ele : json.get("quest_components").getAsJsonArray()) {
			QuestComponent component = new QuestComponent(npcName, displayName, entityType, ele, parent);
			mComponents.add(component);
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		Cutscene cutscene = Core.getInstance().mCutsceneManager.getCutscene(mCutsceneId);

		if (cutscene != null) {
			Core.getInstance().mCutsceneManager.playCutscene(player, cutscene, () -> {
				for (QuestComponent component : mComponents) {
					component.doActionsIfPrereqsMet(plugin, player, npcEntity);
				}
			});
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
		return Collections.emptyList();
	}

	@Override
	public List<QuestComponent> getQuestComponents(Entity entity) {
		List<QuestComponent> components = new ArrayList<>();

		for (QuestComponent component : mComponents) {
			if (component.getPrerequisites() == null || component.getPrerequisites().prerequisiteMet(entity, entity)) {
				components.add(component);
			}
		}

		return components;
	}
}
