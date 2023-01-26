package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import java.util.ArrayList;
import java.util.Iterator;

import com.google.gson.JsonObject;
import me.Novalescent.Constants;
import me.Novalescent.mobs.npcs.RPGNPC;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

public class DialogText implements DialogBase {
	private String mDisplayName;
	private final ArrayList<String> mText = new ArrayList<>();

	public DialogText(String displayName, JsonElement element) throws Exception {
		mDisplayName = displayName;

		if (element.isJsonPrimitive()) {
			mText.add(element.getAsString());
		} else if (element.isJsonArray()) {
			Iterator<JsonElement> iter = element.getAsJsonArray().iterator();
			while (iter.hasNext()) {
				mText.add(iter.next().getAsString());
			}
		} else if (element.isJsonObject()) {
			JsonObject object = element.getAsJsonObject();
			mDisplayName = object.get("speaker").getAsString();
			mText.add(object.get("text").getAsString());
		} else {
			throw new Exception("text value is neither an array nor a string!");
		}
	}

	@Override
	public void sendDialog(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		String name = mDisplayName;
		if (npcEntity != null && npcEntity.hasMetadata(Constants.NPC_METAKEY)) {
			RPGNPC npc = (RPGNPC) npcEntity.getMetadata(Constants.NPC_METAKEY).get(0).value();
			name = ChatColor.stripColor(npc.mNameStand.getCustomName());
		}
		for (String text : mText) {
			if (!text.trim().isEmpty()) {
				MessagingUtils.sendNPCMessage(player, name, text);
			}
		}
	}
}
