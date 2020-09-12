package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import me.Novalescent.Core;
import me.Novalescent.items.trades.Trade;
import me.Novalescent.items.trades.TradeManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ActionTrade implements ActionBase {

	private String mTrade;
	public ActionTrade(JsonElement value) throws Exception {
		mTrade = value.getAsString();
		if (mTrade == null) {
			throw new Exception("trade value is not a string!");
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		TradeManager tradeManager = Core.getInstance().mTradeManager;
		Trade trade = tradeManager.getTrade(mTrade);

		if (trade != null) {
			tradeManager.openTrade(player, trade);
		} else {
			player.sendMessage(ChatColor.RED + "The trade for this interaction was not found. Contact an admin!");
		}
	}
}
