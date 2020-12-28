package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import me.Novalescent.Core;
import me.Novalescent.mobs.spells.scripted.actions.SpellActions;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ActionRunActions implements ActionBase {

	public boolean mOnPlayer;
	public double mYOffset;

	public JsonElement mActionsJson;
	public ActionRunActions(JsonElement value) throws Exception {
		JsonObject json = value.getAsJsonObject();

		mOnPlayer = json.get("on_player").getAsBoolean();
		mYOffset = json.get("y_offset").getAsDouble();

		mActionsJson = json.get("spell_actions");
	}
	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		try {
			SpellActions actions = new SpellActions(Core.getInstance(), null, mActionsJson);
			if (mOnPlayer) {
				actions.doActions(player, player.getLocation().add(0, mYOffset, 0));
			} else {
				if (npcEntity != null) {
					actions.doActions(npcEntity, npcEntity.getLocation().add(0, mYOffset, 0));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
