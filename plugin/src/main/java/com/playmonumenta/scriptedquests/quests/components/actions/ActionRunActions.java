package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import me.Novalescent.mobs.spells.scripted.actions.SpellActions;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ActionRunActions implements ActionBase {

	public boolean mOnPlayer;
	public double mYOffset;
	public SpellActions mActions;
	public ActionRunActions(JsonElement value) throws Exception {
		JsonObject json = value.getAsJsonObject();

		mOnPlayer = json.get("on_player").getAsBoolean();
		mYOffset = json.get("y_offset").getAsDouble();

		mActions = new SpellActions(null, null, json.get("spell_actions"));
	}
	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		if (mOnPlayer) {
			mActions.doActions(player, player.getLocation().add(0, mYOffset, 0));
		} else {
			if (npcEntity != null) {
				mActions.doActions(npcEntity, npcEntity.getLocation().add(0, mYOffset, 0));
			}
		}
	}
}
