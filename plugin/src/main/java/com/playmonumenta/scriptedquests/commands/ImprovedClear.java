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
import io.github.jorelali.commandapi.api.arguments.StringArgument;
import io.github.jorelali.commandapi.api.arguments.TextArgument;

public class ImprovedClear {
	 public static void register() {
         CommandPermission perms = CommandPermission.fromString("scriptedquests.improvedclear");
         LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
         String[] aliases = {"iclear"};

         arguments.put("target", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));
         arguments.put("name", new TextArgument());

         CommandAPI.getInstance().register("improvedclear", perms, aliases, arguments, (sender, args) -> {
             return clearInventory((Player)args[0], (String)args[1], -1, true, "");
         });

         arguments.put("maxAmount", new IntegerArgument());

         CommandAPI.getInstance().register("improvedclear", perms, aliases, arguments, (sender, args) -> {
             return clearInventory((Player)args[0], (String)args[1], (Integer)args[2], true, "");
         });

         arguments.put("clearShulkers", new BooleanArgument());

         CommandAPI.getInstance().register("improvedclear", perms, aliases, arguments, (sender, args) -> {
             return clearInventory((Player)args[0], (String)args[1], (Integer)args[2], (boolean)args[3], "");
         });

         arguments.put("shulkerLore", new StringArgument());

         CommandAPI.getInstance().register("improvedclear", perms, aliases, arguments, (sender, args) -> {
             return clearInventory((Player)args[0], (String)args[1], (Integer)args[2], (boolean)args[3], (String)args[4]);
         });
     }


	 private static int clearInventory(Player player, String itemName, int maxAmount, boolean clearShulkers, String shulkerLore) {
         int count = 0;

         Inventory sInventory = player.getInventory();
         for (int i = 0; i < sInventory.getSize(); i++) {
             ItemStack item = sInventory.getItem(i);
             if (item != null && item.hasItemMeta()) {

                 if (MaterialUtils.shulkerTypes.contains(item.getType())) {
                     // This is a shulker box
                     if (clearShulkers && InventoryUtils.testForItemWithLore(item, shulkerLore)) {
                         // We are clearing shulkers and this specific shulker matches the search lore text (or none was provided)
                         count = clearItemsFromShulkerBox(item, itemName, maxAmount, count);
                     }
                 } else {
                     // Not a shulker box
                     if (InventoryUtils.testForItemWithName(item, itemName)) {
                         // Item matches
                         if (maxAmount != 0 && count < maxAmount) {
                             // Clear the item
                        	 if (sInventory.getItem(i).getAmount() > maxAmount - count) {
    							 item.setAmount(item.getAmount() - (maxAmount - count));
    						 } else {
    							 sInventory.clear(i);
    						 }
                         }
                         count += item.getAmount();
                     }
                 }

                 if (maxAmount > 0 && count >= maxAmount) {
                     // Enough items have been cleared
                     return maxAmount;
                 }
             }
         }

         return count;
     }

	 private static int clearItemsFromShulkerBox(ItemStack shulkerItem, String itemName, int maxAmount, int count) {
		 BlockStateMeta shulkerMeta = (BlockStateMeta)shulkerItem.getItemMeta();
		 ShulkerBox shulkerBox = (ShulkerBox)shulkerMeta.getBlockState();
		 Inventory sInventory = shulkerBox.getInventory();

		 for (int i = 0; i < sInventory.getSize(); i++) {
			 ItemStack item = sInventory.getItem(i);

			 if (item != null && item.hasItemMeta()) {
				 //Is the item the one we are looking for?
				 if (InventoryUtils.testForItemWithName(item, itemName)) {
					 //Make sure you actually need to clear the items
					 if (maxAmount != 0 && count < maxAmount) {
						 //Check if the itemstack size is larger than needed, then set it to what it would be if you took away that many. Or just remove the stack if its not more than needed.
						 if (sInventory.getItem(i).getAmount() > maxAmount - count) {
							 item.setAmount(item.getAmount() - (maxAmount - count));
						 } else {
							 sInventory.clear(i);
						 }
					 }
					 count += item.getAmount();
				 }
				 if (maxAmount > 0 && count >= maxAmount) {
					 shulkerMeta.setBlockState(shulkerBox);
	                 shulkerItem.setItemMeta(shulkerMeta);
					 return maxAmount;
				 }
			 }
		 }
		 shulkerMeta.setBlockState(shulkerBox);
         shulkerItem.setItemMeta(shulkerMeta);
         return count;
     }
 }
