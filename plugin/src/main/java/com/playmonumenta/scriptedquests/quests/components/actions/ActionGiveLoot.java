package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.utils.InventoryUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.Random;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ActionGiveLoot implements ActionBase {
	private String mLootPath;
	private Random mRandom;

	public ActionGiveLoot(JsonElement element) throws Exception {
		mLootPath = element.getAsString();
		if (mLootPath == null) {
			throw new Exception("Command value is not a string!");
		}
		mRandom = new Random();
	}

	@Override
	public void doActions(QuestContext context) {
		try {
			InventoryUtils.giveLootTableContents(context.getPlayer(), mLootPath, mRandom, false);
		} catch (Exception e) {
			context.getPlayer().sendMessage(Component.text("BUG! Server failed to give you loot from the table '" + mLootPath + "'", NamedTextColor.RED));
			context.getPlayer().sendMessage(Component.text("Please hover over the following message, take a screenshot, and report this to a moderator", NamedTextColor.RED));
			MessagingUtils.sendStackTrace(context.getPlayer(), e);
		}
	}
}
