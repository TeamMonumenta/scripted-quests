package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class DialogRawRandomText implements DialogBase {
	private ArrayList<String> mText = new ArrayList<String>();
	private Random mRandom = new Random();

	public DialogRawRandomText(JsonElement element) throws Exception {

		if (element.isJsonPrimitive()) {
			mText.add(element.getAsString());
		} else if (element.isJsonArray()) {
			Iterator<JsonElement> iter = element.getAsJsonArray().iterator();
			while (iter.hasNext()) {
				mText.add(iter.next().getAsString());
			}
		} else {
			throw new Exception("random_text value is neither an array nor a string!");
		}
	}

	@Override
	public void sendDialog(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		int idx = mRandom.nextInt(mText.size());
		String text = mText.get(idx);

		if (!text.isEmpty()) {
			MessagingUtils.sendRawMessage(player, text);
		}

	}
}

