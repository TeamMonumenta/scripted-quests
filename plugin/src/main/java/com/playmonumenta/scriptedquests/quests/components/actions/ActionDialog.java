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
	private final ArrayList<DialogBase> mDialogs = new ArrayList<>();

	public ActionDialog(@Nullable String npcName, @Nullable String displayName,
	             @Nullable EntityType entityType, JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("dialog value is not an object!");
		}

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();
			switch (key) {
				case "text" -> mDialogs.add(new DialogText(displayName, ent.getValue()));
				case "raw_text" -> mDialogs.add(new DialogRawText(ent.getValue()));
				case "clickable_text" -> mDialogs.add(new DialogClickableText(npcName, displayName, entityType, ent.getValue()));
				case "random_text" -> mDialogs.add(new DialogRandomText(displayName, ent.getValue()));
				case "all_in_one_text" -> mDialogs.add(new DialogAllInOneText(npcName, ent.getValue()));
				default -> throw new Exception("Unknown dialog key: '" + key + "'");
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
