package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import me.Novalescent.Core;
import me.Novalescent.items.enchantments.core.EnchantAltar;
import me.Novalescent.items.enchantments.core.EnchantmentManager;
import me.Novalescent.items.reforges.ReforgeMenu;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ActionOpenAltar implements ActionBase {

	private String mAltar;
	public ActionOpenAltar(JsonElement value) throws Exception {
		mAltar = value.getAsString();
		if (mAltar == null) {
			throw new Exception("altar value is not a string!");
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		EnchantmentManager manager = Core.getInstance().mEnchantmentManager;
		EnchantAltar altar = manager.getAltar(mAltar);

		if (altar != null) {
			altar.openMenu(player);
		}
	}

}
