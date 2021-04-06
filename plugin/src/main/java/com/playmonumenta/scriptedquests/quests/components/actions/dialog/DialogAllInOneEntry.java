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

import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public class DialogAllInOneEntry implements DialogBase {

	private String mText = null;
	// It needs an initialized value
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

			JsonElement value = object.get(key);
			if (value == null) {
				throw new Exception("clickable_text value for key '" + key + "' is not parseable!");
			}

			if (!key.equals("hover_text") && !key.equals("click_action") && !key.equals("player_text") && !key.equals("actual_text")) {
				throw new Exception("Unknown clickable_text key: " + key);
			}

			if (key.equals("player_text")) {
				mText = ent.getValue().getAsString();
			}

			if (key.equals("click_action")) {
				if (mComponent.clickEvent() != null) {
					throw new Exception("There can only be one on click event!");
				}
				JsonObject clickObject = ent.getValue().getAsJsonObject();
				for (Entry<String, JsonElement> clickEnt : clickObject.entrySet()) {
					if (!clickEnt.getKey().equals("click_command") && !clickEnt.getKey().equals("click_url")) {
						throw new Exception("The click action is not a command or a url!");
					}
					if (mComponent.clickEvent() != null) {
						throw new Exception("There may only be one click action per dialogue string!");
					}

					if (clickEnt.getKey().equals("click_command")) {
						ClickEvent event = ClickEvent.runCommand(clickEnt.getValue().getAsString());
						mComponent = mComponent.clickEvent(event);
					}

					if (clickEnt.getKey().equals("click_url")) {
						ClickEvent event = ClickEvent.openUrl(clickEnt.getValue().getAsString());
						mComponent = mComponent.clickEvent(event);
					}
				}
			}

			if (key.equals("hover_text")) {
				HoverEvent<Component> event = HoverEvent.showText(MessagingUtils.LEGACY_SERIALIZER.deserialize(((ent.getValue().getAsString().replace("&", "§")))));
				mComponent = mComponent.hoverEvent(event);
			}

			if (key.equals("actual_text")) {
				mComponent = MessagingUtils.LEGACY_SERIALIZER.deserialize((ent.getValue().getAsString().replace("&", "§")));
			}
		}
	}

	@Override
	public void sendDialog(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		if (mText != null || !mText.equals("")) {
			if (mNpcName != null || !mNpcName.equals("")) {
				MessagingUtils.sendNPCMessage(player, mNpcName, mText);
			} else {
				player.sendMessage(mText.replace("&", "§"));
			}
		}
		player.sendMessage(Identity.identity(player.getUniqueId()), mComponent);
	}
}
