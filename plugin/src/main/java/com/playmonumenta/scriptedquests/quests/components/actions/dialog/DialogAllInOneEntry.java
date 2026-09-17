package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.Map.Entry;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.jetbrains.annotations.Nullable;

public class DialogAllInOneEntry implements DialogBase {

	private String mText = ""; // this will always be initialized unless an exception is thrown
	private @Nullable ClickEvent mClick = null;
	private @Nullable HoverEvent<Component> mHover = null;
	private final String mNPCName;
	private final boolean mMiniMessage;

	public DialogAllInOneEntry(String npcName, JsonElement element, boolean miniMessage) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("dialog value is not an object!");
		}
		if (!object.has("actual_text")) {
			throw new Exception("all_in_one_text requires actual_text value");
		}

		mNPCName = npcName;
		mMiniMessage = miniMessage;

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			JsonElement value = ent.getValue();
			if (value == null) {
				throw new Exception("clickable_text value for key '" + key + "' is not parseable!");
			}

			switch (key) {
				case "click_action" -> {
					JsonObject clickObject = ent.getValue().getAsJsonObject();
					for (Entry<String, JsonElement> clickEnt : clickObject.entrySet()) {
						if (mClick != null) {
							throw new Exception("There can only be one click_action event!");
						}

						switch (clickEnt.getKey()) {
							case "click_command" -> mClick = ClickEvent.runCommand(value.getAsString());
							case "click_url" -> mClick = ClickEvent.openUrl(value.getAsString());
							default -> throw new Exception("The click action is not a command or a url!");
						}
					}
				}
				case "hover_text" -> mHover = HoverEvent.showText(MessagingUtils.deserialize(value.getAsString(), miniMessage));
				case "actual_text" -> mText = object.get("actual_text").getAsString();
				default -> throw new Exception("Unknown all_in_one_text key: " + key);
			}
		}
	}

	@Override
	public void sendDialog(QuestContext context) {
		MessagingUtils.sendNPCMessage(context.getPlayer(), mNPCName, mText, mMiniMessage,
			component -> component.clickEvent(mClick).hoverEvent(mHover));
	}
}
