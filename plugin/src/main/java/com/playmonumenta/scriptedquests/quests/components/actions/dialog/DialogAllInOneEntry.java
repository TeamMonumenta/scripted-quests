package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public class DialogAllInOneEntry implements DialogBase {

	private String mText = null;
	private Component mComponent;
	private String mNpcName = null;

	public DialogAllInOneEntry(String npcName, String displayName, EntityType entityType, JsonElement element) throws Exception {

		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("dialog value is not an object!");
		}

		if (npcName != null) {
			mNpcName = npcName;
		}

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("hover_text") && !key.equals("click_command") && !key.equals("player_text") && !key.equals("click_url") && !key.equals("actual_text")) {
				throw new Exception("Unknown clickable_text key: " + key);
			}

			if (key.equals("player_text")) {
				mText = ent.getValue().getAsString();
			}

			if (key.equals("click_command")) {
				if (mComponent.clickEvent() != null) {
					throw new Exception("There can only be one on click event!");
				}
				ClickEvent event = ClickEvent.runCommand(ent.getValue().getAsString());
				mComponent.clickEvent(event);
			}

			if (key.equals("hover_text")) {
				HoverEvent<Component> event = HoverEvent.showText(Component.text(ent.getValue().getAsString()));
				mComponent.hoverEvent(event);
			}

			if (key.equals("click_url")) {
				if (mComponent.clickEvent() != null) {
					throw new Exception("There can only be one on click event!");
				}
				ClickEvent event = ClickEvent.openUrl(ent.getValue().getAsString());
				mComponent.clickEvent(event);
			}
		}
	}

	@Override
	public void sendDialog(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		if (mText != null || !mText.equals("")) {
			if (mNpcName != null || !mNpcName.equals("")) {
				MessagingUtils.sendNPCMessage(player, mNpcName, mText);
			} else {
				player.sendMessage(mText);
			}
		}
		player.sendMessage(mComponent);
	}
}
