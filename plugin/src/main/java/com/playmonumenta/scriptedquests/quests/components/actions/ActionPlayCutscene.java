package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.quests.components.actions.quest.ActionQuest;
import me.Novalescent.Core;
import me.Novalescent.actorsystem.Scene;
import me.Novalescent.actorsystem.SceneActive;
import me.Novalescent.player.PlayerData;
import me.Novalescent.professions.ProfessionData;
import me.Novalescent.professions.ProfessionType;
import me.Novalescent.utils.FormattedMessage;
import me.Novalescent.utils.MessageFormat;
import me.Novalescent.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActionPlayCutscene implements ActionBase, ActionNested {

	private final String mCutsceneId;

	private Integer mEndingFrame = 999999;
	private final Vector mPosition;
	private final List<QuestComponent> mComponents = new ArrayList<>();
	private final ActionNested mParent;
	public ActionPlayCutscene(String npcName, String displayName, EntityType entityType, JsonElement value, ActionNested parent) throws Exception {
		mParent = parent;
		JsonObject json = value.getAsJsonObject();

		mCutsceneId = json.get("cutscene_id").getAsString();
		if (mCutsceneId == null) {
			throw new Exception("cutscene_id is an invalid string!");
		}

		mPosition = Utils.getVector(json.get("position").getAsString());

		for (JsonElement ele : json.get("quest_components").getAsJsonArray()) {
			QuestComponent component = new QuestComponent(npcName, displayName, entityType, ele, parent);
			mComponents.add(component);
		}

		if (json.has("ending_frame")) {
			mEndingFrame = json.get("ending_frame").getAsInt();
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		Scene scene = Core.getInstance().mSceneManager.getScene(mCutsceneId);

		if (scene != null) {
			Location loc = new Location(player.getWorld(), mPosition.getBlockX(), mPosition.getBlockY(), mPosition.getBlockZ());
			Core.getInstance().mSceneManager.playScene(scene, loc, player, mEndingFrame, () -> {
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
