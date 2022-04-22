package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.components.actions.dialog.DialogAllInOneText;
import com.playmonumenta.scriptedquests.quests.components.actions.dialog.DialogBase;
import com.playmonumenta.scriptedquests.quests.components.actions.dialog.DialogClickableText;
import com.playmonumenta.scriptedquests.quests.components.actions.dialog.DialogRandomText;
import com.playmonumenta.scriptedquests.quests.components.actions.dialog.DialogRawText;
import com.playmonumenta.scriptedquests.quests.components.actions.dialog.DialogText;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public class ActionDialog implements ActionBase {
	private ArrayList<DialogBase> mDialogs = new ArrayList<DialogBase>();

	public ActionDialog(@Nullable String npcName, @Nullable String displayName,
	             @Nullable EntityType entityType, JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("dialog value is not an object!");
		}

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (key.equals("text")) {
				if (displayName == null) {
					throw new Exception("Tried to create text dialog but displayName was null");
				}
				mDialogs.add(new DialogText(displayName, ent.getValue()));
			} else if (key.equals("raw_text")) {
				mDialogs.add(new DialogRawText(ent.getValue()));
			} else if (key.equals("clickable_text")) {
				mDialogs.add(new DialogClickableText(npcName, displayName, entityType, ent.getValue()));
			} else if (key.equals("random_text")) {
				if (displayName == null) {
					throw new Exception("Tried to create random text dialog but displayName was null");
				}
				mDialogs.add(new DialogRandomText(displayName, ent.getValue()));
			} else if (key.equals("all_in_one_text")) {
				mDialogs.add(new DialogAllInOneText(npcName, ent.getValue()));
			} else {
				throw new Exception("Unknown dialog key: '" + key + "'");
			}
		}
	}

	@Override
	public void doAction(QuestContext context) {
		// handle packet stuff
		for (DialogBase dialog : mDialogs) {
			dialog.sendDialog(context);
		}
	}

	@Override
	public JsonElement serializeForClientAPI(QuestContext context) {
		return JsonObjectBuilder.get()
			.add("type", "dialog")
			.add("dialog", mDialogs.stream().map(v -> v.serializeForClientAPI(context))
				.collect(Collectors.toList()))
			.build();
	}
}
