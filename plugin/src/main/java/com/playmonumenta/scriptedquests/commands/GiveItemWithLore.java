package com.playmonumenta.scriptedquests.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import com.playmonumenta.scriptedquests.utils.InventoryUtils;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GiveItemWithLore {
	@SuppressWarnings("unchecked")
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("scriptedquests.giveitemwithlore");

		new CommandAPICommand("giveitemwithlore")
			.withPermission(perms)
			.withArguments(
				new EntitySelectorArgument("players", EntitySelector.MANY_PLAYERS),
				new ItemStackArgument("item"),
				new IntegerArgument("count"),
				new TextArgument("lore")
			)
			.executes((sender, args) -> {
				giveLoot((Collection<Player>)args[0], (ItemStack)args[1], (Integer)args[2], (String)args[3]);
			})
			.register();

	}

	private static void giveLoot(Collection<Player> players, ItemStack item, int count, String lore) throws WrapperCommandSyntaxException {
		item.setAmount(count);
		final List<String> itemLore;
		ItemMeta meta = item.getItemMeta();

		if (meta == null) {
			CommandAPI.fail("Item missing meta, can not add lore");
		}
		if (meta != null && meta.hasLore()) {
			itemLore = meta.getLore();
		} else {
			itemLore = new ArrayList<String>();
		}

		for (String addLore : lore.split("\\\\n")) {
			itemLore.add(ChatColor.translateAlternateColorCodes('&', addLore));
		}

		meta.setLore(itemLore);
		item.setItemMeta(meta);

		List<ItemStack> items = new ArrayList<ItemStack>(1);
		items.add(item);

		for (Player player : players) {
			InventoryUtils.giveItems(player, items, false);
		}
	}
}
