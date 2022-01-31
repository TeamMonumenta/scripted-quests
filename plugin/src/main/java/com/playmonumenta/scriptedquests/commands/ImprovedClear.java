package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.utils.InventoryUtils;
import com.playmonumenta.scriptedquests.utils.MaterialUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

public class ImprovedClear {
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("scriptedquests.improvedclear");
		String[] aliases = {"iclear"};

		new CommandAPICommand("improvedclear")
			.withPermission(perms)
			.withArguments(new EntitySelectorArgument("target", EntitySelectorArgument.EntitySelector.ONE_PLAYER))
			.withArguments(new TextArgument("name"))
			.withAliases(aliases)
			.executes((sender, args) -> {
				// Make sure shulker boxes are closed so they can be clear'd
				((Player)args[0]).closeInventory();
				return clearInventory(((Player)args[0]).getInventory(), (String)args[1], -1, true, "", 0);
			})
			.register();

		new CommandAPICommand("improvedclear")
			.withPermission(perms)
			.withArguments(new EntitySelectorArgument("target", EntitySelectorArgument.EntitySelector.ONE_PLAYER))
			.withArguments(new TextArgument("name"))
			.withArguments(new IntegerArgument("maxAmount"))
			.withAliases(aliases)
			.executes((sender, args) -> {
				// Make sure shulker boxes are closed so they can be clear'd
				((Player)args[0]).closeInventory();
				return clearInventory(((Player)args[0]).getInventory(), (String)args[1], (Integer)args[2], true, "", 0);
			})
			.register();

		new CommandAPICommand("improvedclear")
			.withPermission(perms)
			.withArguments(new EntitySelectorArgument("target", EntitySelectorArgument.EntitySelector.ONE_PLAYER))
			.withArguments(new TextArgument("name"))
			.withArguments(new IntegerArgument("maxAmount"))
			.withArguments(new BooleanArgument("clearShulkers"))
			.withAliases(aliases)
			.executes((sender, args) -> {
				// Make sure shulker boxes are closed so they can be clear'd
				((Player)args[0]).closeInventory();
				return clearInventory(((Player)args[0]).getInventory(), (String)args[1], (Integer)args[2], (Boolean)args[3], "", 0);
			})
			.register();

		new CommandAPICommand("improvedclear")
			.withPermission(perms)
			.withArguments(new EntitySelectorArgument("target", EntitySelectorArgument.EntitySelector.ONE_PLAYER))
			.withArguments(new TextArgument("name"))
			.withArguments(new IntegerArgument("maxAmount"))
			.withArguments(new BooleanArgument("clearShulkers"))
			.withArguments(new TextArgument("shulkerLore"))
			.withAliases(aliases)
			.executes((sender, args) -> {
				// Make sure shulker boxes are closed so they can be clear'd
				((Player)args[0]).closeInventory();
				return clearInventory(((Player)args[0]).getInventory(), (String)args[1], (Integer)args[2], (Boolean)args[3], (String)args[4], 0);
			})
			.register();
	}


	private static int clearInventory(Inventory inv, String itemName, int maxAmount, boolean clearShulkers, String shulkerLore, int count) {
		for (int i = 0; i < inv.getSize(); i++) {
			ItemStack item = inv.getItem(i);
			if (item != null && item.hasItemMeta()) {

				if (MaterialUtils.shulkerTypes.contains(item.getType())) {
					// This is a shulker box
					if (clearShulkers && InventoryUtils.testForItemWithLore(item, shulkerLore, false)) {
						// We are clearing shulkers and this specific shulker matches the search lore text (or none was provided)
						BlockStateMeta shulkerMeta = (BlockStateMeta) item.getItemMeta();
						ShulkerBox shulkerBox = (ShulkerBox) shulkerMeta.getBlockState();

						// Recurse!
						count = clearInventory(shulkerBox.getInventory(), itemName, maxAmount, clearShulkers, shulkerLore, count);

						shulkerMeta.setBlockState(shulkerBox);
						item.setItemMeta(shulkerMeta);
					}
				} else {
					// Not a shulker box
					if (InventoryUtils.testForItemWithName(item, itemName, false)) {
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
