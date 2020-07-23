package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import com.playmonumenta.scriptedquests.utils.InventoryUtils;
import com.playmonumenta.scriptedquests.utils.MaterialUtils;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.BooleanArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.IntegerArgument;
import io.github.jorelali.commandapi.api.arguments.TextArgument;

public class ImprovedClear {
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("scriptedquests.improvedclear");
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		String[] aliases = {"iclear"};

		arguments.put("target", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));
		arguments.put("name", new TextArgument());

		CommandAPI.getInstance().register("improvedclear", perms, aliases, arguments, (sender, args) -> {
			// Make sure shulker boxes are closed so they can be clear'd
			((Player)args[0]).closeInventory();
			return clearInventory(((Player)args[0]).getInventory(), (String)args[1], -1, true, "", 0);
		});

		arguments.put("maxAmount", new IntegerArgument());

		CommandAPI.getInstance().register("improvedclear", perms, aliases, arguments, (sender, args) -> {
			// Make sure shulker boxes are closed so they can be clear'd
			((Player)args[0]).closeInventory();
			return clearInventory(((Player)args[0]).getInventory(), (String)args[1], (Integer)args[2], true, "", 0);
		});

		arguments.put("clearShulkers", new BooleanArgument());

		CommandAPI.getInstance().register("improvedclear", perms, aliases, arguments, (sender, args) -> {
			// Make sure shulker boxes are closed so they can be clear'd
			((Player)args[0]).closeInventory();
			return clearInventory(((Player)args[0]).getInventory(), (String)args[1], (Integer)args[2], (Boolean)args[3], "", 0);
		});

		arguments.put("shulkerLore", new TextArgument());

		CommandAPI.getInstance().register("improvedclear", perms, aliases, arguments, (sender, args) -> {
			// Make sure shulker boxes are closed so they can be clear'd
			((Player)args[0]).closeInventory();
			return clearInventory(((Player)args[0]).getInventory(), (String)args[1], (Integer)args[2], (Boolean)args[3], (String)args[4], 0);
		});
	}


	private static int clearInventory(Inventory inv, String itemName, int maxAmount, boolean clearShulkers, String shulkerLore, int count) {
		for (int i = 0; i < inv.getSize(); i++) {
			ItemStack item = inv.getItem(i);
			if (item != null && item.hasItemMeta()) {

				if (MaterialUtils.shulkerTypes.contains(item.getType())) {
					// This is a shulker box
					if (clearShulkers && InventoryUtils.testForItemWithLore(item, shulkerLore)) {
						// We are clearing shulkers and this specific shulker matches the search lore text (or none was provided)
						BlockStateMeta shulkerMeta = (BlockStateMeta)item.getItemMeta();
						ShulkerBox shulkerBox = (ShulkerBox)shulkerMeta.getBlockState();

						// Recurse!
						count = clearInventory(shulkerBox.getInventory(), itemName, maxAmount, clearShulkers, shulkerLore, count);

						shulkerMeta.setBlockState(shulkerBox);
						item.setItemMeta(shulkerMeta);
					}
				} else {
					// Not a shulker box
					if (InventoryUtils.testForItemWithName(item, itemName)) {
						// Item matches
						if (maxAmount != 0 && (count < maxAmount || maxAmount == -1)) {
							// Clear the item
							if (inv.getItem(i).getAmount() > maxAmount - count && maxAmount != -1) {
								item.setAmount(item.getAmount() - (maxAmount - count));
								count += (maxAmount - count);
							} else {
								count += item.getAmount();
								inv.clear(i);
							}
						} else {
							count += item.getAmount();
						}
					}
				}

				if (maxAmount > 0 && count >= maxAmount) {
					// Enough items have been cleared
					return count;
				}
			}
		}

		return count;
	}
}
