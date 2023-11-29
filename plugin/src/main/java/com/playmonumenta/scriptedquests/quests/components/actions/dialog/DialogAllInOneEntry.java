package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import java.util.Map.Entry;
import java.util.Set;

public class DialogAllInOneEntry implements DialogBase {

	// It needs an initialized value
	private Component mComponent;
	private String mNPCName;

	public DialogAllInOneEntry(String npcName, JsonElement element) throws Exception {

		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("dialog value is not an object!");
		}

		mNPCName = npcName;

		mComponent = MessagingUtils.AMPERSAND_SERIALIZER.deserialize(object.get("actual_text").getAsString().replace("§", "&"));

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
				if (mComponent.clickEvent() != null) {
					throw new Exception("There can only be one on click event!");
				}
				JsonObject clickObject = ent.getValue().getAsJsonObject();
				for (Entry<String, JsonElement> clickEnt : clickObject.entrySet()) {
					if (!clickEnt.getKey().equals("click_command") && !clickEnt.getKey().equals("click_url")) {
						throw new Exception("The click action is not a command or a url!");
					}

					if (clickEnt.getKey().equals("click_command")) {
						ClickEvent event = ClickEvent.runCommand(value.getAsString());
						mComponent = mComponent.clickEvent(event);
					}

					if (clickEnt.getKey().equals("click_url")) {
						ClickEvent event = ClickEvent.openUrl(value.getAsString());
						mComponent = mComponent.clickEvent(event);
					}
				}
			}

			if (key.equals("hover_text")) {
				HoverEvent<Component> event = HoverEvent.showText(MessagingUtils.AMPERSAND_SERIALIZER.deserialize(value.getAsString().replace("§", "&")));
				mComponent = mComponent.hoverEvent(event);
			}
		}
	}

	@Override
	public void sendDialog(QuestContext context) {
		MessagingUtils.sendNPCMessage(context.getPlayer(), mNPCName, mComponent.replaceText(TextReplacementConfig.builder().match("@S").replacement(context.getPlayer().getName()).build()));
	}

	@Override
	public JsonElement serializeForClientAPI(QuestContext context) {
		return JsonObjectBuilder.get()
			.add("text", TextComponent.ofChildren(mComponent.replaceText(TextReplacementConfig.builder()
				.match("@S").replacement(context.getPlayer().getName()).build())).content())
			.add("npc_name", mNPCName)
			.build();
	}
}
