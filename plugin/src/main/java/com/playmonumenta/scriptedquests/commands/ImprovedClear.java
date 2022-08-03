package com.playmonumenta.scriptedquests.commands;

import java.util.LinkedHashMap;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.*;
import me.Novalescent.items.types.RPGItem;
import me.Novalescent.utils.Utils;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import com.playmonumenta.scriptedquests.utils.InventoryUtils;
import com.playmonumenta.scriptedquests.utils.MaterialUtils;

public class ImprovedClear {
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("scriptedquests.improvedclear");
		String[] aliases = {"iclear"};

		new CommandAPICommand("improvedclear")
			.withPermission(perms)
			.withArguments(
				new EntitySelectorArgument("target", EntitySelector.ONE_PLAYER),
				new TextArgument("name"),
				new BooleanArgument("rpgItem")
			)
			.withAliases(aliases)
			.executes((sender, args) -> {
				// Make sure shulker boxes are closed so they can be clear'd
				((Player)args[0]).closeInventory();
				Utils.updateInventory((Player) args[0]);
				return clearInventory(((Player)args[0]).getInventory(), (String)args[1], -1, true, "",  0, (boolean) args[2]);
			})
			.register();

		new CommandAPICommand("improvedclear")
			.withPermission(perms)
			.withArguments(
				new EntitySelectorArgument("target", EntitySelector.ONE_PLAYER),
				new TextArgument("name"),
				new BooleanArgument("rpgItem"),
				new IntegerArgument("maxAmount")
			)
			.withAliases(aliases)
			.executes((sender, args) -> {
				// Make sure shulker boxes are closed so they can be clear'd
				((Player)args[0]).closeInventory();
				Utils.updateInventory((Player) args[0]);
				return clearInventory(((Player)args[0]).getInventory(), (String)args[1], (Integer)args[3], true, "", 0, (boolean) args[2]);
			})
			.register();

		new CommandAPICommand("improvedclear")
			.withPermission(perms)
			.withArguments(
				new EntitySelectorArgument("target", EntitySelector.ONE_PLAYER),
				new TextArgument("name"),
				new BooleanArgument("rpgItem"),
				new IntegerArgument("maxAmount"),
				new BooleanArgument("clearShulkers")
			)
			.withAliases(aliases)
			.executes((sender, args) -> {
				// Make sure shulker boxes are closed so they can be clear'd
				((Player)args[0]).closeInventory();
				Utils.updateInventory((Player) args[0]);
				return clearInventory(((Player)args[0]).getInventory(), (String)args[1], (Integer)args[3], (Boolean)args[4], "", 0, (boolean) args[2]);
			})
			.register();

		new CommandAPICommand("improvedclear")
			.withPermission(perms)
			.withArguments(
				new EntitySelectorArgument("target", EntitySelector.ONE_PLAYER),
				new TextArgument("name"),
				new BooleanArgument("rpgItem"),
				new IntegerArgument("maxAmount"),
				new BooleanArgument("clearShulkers"),
				new TextArgument("shulkerLore")
			)
			.withAliases(aliases)
			.executes((sender, args) -> {
				// Make sure shulker boxes are closed so they can be clear'd
				((Player)args[0]).closeInventory();
				Utils.updateInventory((Player) args[0]);
				return clearInventory(((Player)args[0]).getInventory(), (String)args[1], (Integer)args[3], (Boolean)args[4], (String)args[5], 0, (boolean) args[2]);
			})
			.register();



	}


	private static int clearInventory(Inventory inv, String itemName, int maxAmount, boolean clearShulkers, String shulkerLore, int count, boolean isRPG) {
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
						count = clearInventory(shulkerBox.getInventory(), itemName, maxAmount, clearShulkers, shulkerLore, count, isRPG);

						shulkerMeta.setBlockState(shulkerBox);
						item.setItemMeta(shulkerMeta);
					}
				} else {
					// Not a shulker box
					RPGItem rpgItem = Utils.getRPGItem(item);
					if ((!isRPG && InventoryUtils.testForItemWithName(item, itemName))
					|| (isRPG && rpgItem != null && rpgItem.mId.equalsIgnoreCase(itemName))) {
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
