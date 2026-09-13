package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.Map.Entry;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public class DialogAllInOneEntry implements DialogBase {

	private final String mText;
	private ClickEvent mClick = null;
	private HoverEvent<Component> mHover = null;
	private final String mNPCName;
	private final boolean mMiniMessage;

	public DialogAllInOneEntry(String npcName, JsonElement element, boolean miniMessage) throws Exception {
		mMiniMessage = miniMessage;

		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("dialog value is not an object!");
		}

		mNPCName = npcName;

		mText = object.get("actual_text").getAsString();

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			JsonElement value = ent.getValue();
			if (value == null) {
				throw new Exception("clickable_text value for key '" + key + "' is not parseable!");
			}

			if (!key.equals("hover_text") && !key.equals("click_action") && !key.equals("player_text") && !key.equals("actual_text")) {
				throw new Exception("Unknown clickable_text key: " + key);
			}


			if (key.equals("click_action")) {
				JsonObject clickObject = ent.getValue().getAsJsonObject();
				for (Entry<String, JsonElement> clickEnt : clickObject.entrySet()) {
					if (mClick != null) {
						throw new Exception("There can only be one on click event!");
					}

					if (!clickEnt.getKey().equals("click_command") && !clickEnt.getKey().equals("click_url")) {
						throw new Exception("The click action is not a command or a url!");
					}

					if (clickEnt.getKey().equals("click_command")) {
						mClick = ClickEvent.runCommand(value.getAsString());
					}

					if (clickEnt.getKey().equals("click_url")) {
						mClick = ClickEvent.openUrl(value.getAsString());
					}
				}
			}

			if (key.equals("hover_text")) {
				mHover = HoverEvent.showText(MessagingUtils.deserialize(value.getAsString(), miniMessage));
			}
		}
	}

	@Override
	public void sendDialog(QuestContext context) {
		MessagingUtils.sendNPCMessage(context.getPlayer(), mNPCName, mText, mMiniMessage,
			component -> component.clickEvent(mClick).hoverEvent(mHover));
	}
}
