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

		EntitySelectorArgument.OnePlayer targetArg = new EntitySelectorArgument.OnePlayer("target");
		TextArgument nameArg = new TextArgument("name");
		IntegerArgument maxAmountArg = new IntegerArgument("maxAmount");
		BooleanArgument clearShulkersArg = new BooleanArgument("clearShulkers");
		TextArgument shulkerLoreArg = new TextArgument("shulkerLore");

		new CommandAPICommand("improvedclear")
			.withPermission(perms)
			.withArguments(targetArg)
			.withArguments(nameArg)
			.withAliases(aliases)
			.executes((sender, args) -> {
				Player player = args.getByArgument(targetArg);
				String itemName = args.getByArgument(nameArg);
				int maxAmount = args.getByArgumentOrDefault(maxAmountArg, -1);
				boolean clearShulkers = args.getByArgumentOrDefault(clearShulkersArg, true);
				String shulkerLore = args.getByArgumentOrDefault(shulkerLoreArg, "");
				// Make sure shulker boxes are closed so they can be clear'd
				player.closeInventory();
				return clearInventory(player.getInventory(), itemName, maxAmount, clearShulkers, shulkerLore, 0);
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
						count = clearInventory(shulkerBox.getInventory(), itemName, maxAmount, true, shulkerLore, count);

						shulkerMeta.setBlockState(shulkerBox);
						item.setItemMeta(shulkerMeta);
					}
				} else {
					// Not a shulker box
					if (InventoryUtils.testForItemWithName(item, itemName, false)) {
						// Item matches
						if (maxAmount != 0 && (count < maxAmount || maxAmount == -1)) {
							// Clear the item
							if (item.getAmount() > maxAmount - count && maxAmount != -1) {
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
