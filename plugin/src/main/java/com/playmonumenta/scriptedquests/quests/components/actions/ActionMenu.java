package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import me.Novalescent.Core;
import me.Novalescent.items.imbuements.ImbuementMenu;
import me.Novalescent.items.refinements.RefinementMenu;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ActionMenu implements ActionBase {

	private String mMenu;
	public ActionMenu(JsonElement value) throws Exception {
		mMenu = value.getAsString();
		if (mMenu == null) {
			throw new Exception("menu value is not a string!");
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		if (mMenu.equalsIgnoreCase("imbuements")) {
			ImbuementMenu.openMenu(Core.getInstance(), player);
		} else if (mMenu.equalsIgnoreCase("refinements")) {
			RefinementMenu.openMenu(Core.getInstance(), player);
		} else {
			player.sendMessage(ChatColor.RED + "The menu that this interaction was supposed to open was not found. Contact an admin!");
		}
	}
}
