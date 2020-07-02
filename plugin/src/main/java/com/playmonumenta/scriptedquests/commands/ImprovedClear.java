package com.playmonumenta.scriptedquests.commands;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

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
	@SuppressWarnings("unchecked")
	public static void register() {
		//TODO: Make this work with actual permissions
		CommandPermission perms = CommandPermission.fromString("scriptedquests.improvedclear");
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		String[] aliases = {"iclear"};

		arguments.put("target", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		arguments.put("name", new TextArgument());

		CommandAPI.getInstance().register("improvedclear", perms, aliases, arguments, (sender, args) -> {
			clearInventory((Collection<Player>)args[0], (String)args[1], -1, true, "");
		});

		arguments.put("maxAmount", new IntegerArgument());

		CommandAPI.getInstance().register("improvedclear", perms, aliases, arguments, (sender, args) -> {
			clearInventory((Collection<Player>)args[0], (String)args[1], (Integer)args[2], true, "");
		});

		arguments.put("clearShulkers", new BooleanArgument());

		CommandAPI.getInstance().register("improvedclear", perms, aliases, arguments, (sender, args) -> {
			clearInventory((Collection<Player>)args[0], (String)args[1], (Integer)args[2], (boolean)args[3], "");
		});

		arguments.put("shulkerLore", new StringArgument());

		CommandAPI.getInstance().register("improvedclear", perms, aliases, arguments, (sender, args) -> {
			clearInventory((Collection<Player>)args[0], (String)args[1], (Integer)args[2], (boolean)args[3], (String)args[4]);
		});
	}


	private static void clearInventory(Collection<Player> players, String item, int maxAmount, boolean clearShulkers, String shulkerLore) {
		for (Player player : players) {
			clearInventory(player, item, maxAmount, clearShulkers, shulkerLore);
		}
	}

	private static void clearInventory(Player player, String itemName, int maxAmount, boolean clearShulkers, String shulkerLock) {
		Inventory sInventory = player.getInventory();
		HashMap<Integer, ItemStack> items = new HashMap<>();
		int itemNum = 0;

		ItemStack[] itemsArray = sInventory.getContents();
		int arrayCount = 0;
		for (int i = 0; i < itemsArray.length; i++) {
			if (itemsArray[i] == null) {
					continue;
			}
			ItemMeta meta = itemsArray[i].getItemMeta();
			if (meta.getLore() == null || meta.getDisplayName() == null) {
				continue;
			} else if (meta.getLore().isEmpty() || meta.getDisplayName().isEmpty()) {
				continue;
			}

			if (InventoryUtils.testForItemWithName(itemsArray[i], itemName)) {
				items.put(arrayCount, itemsArray[i]);
				arrayCount++;
			}
		}

		if (items.size() == 0) {
			if (clearShulkers) {
				Iterator<Material> iterator = MaterialUtils.shulkerTypes.iterator();
				iterateOverShulkerTypes(player, iterator, itemName, maxAmount, shulkerLock, 0);
				return;
			} else {
				return;
			}
		}

		for (int i = 0; i < items.size(); i++) {
			itemNum += items.get(i).getAmount();
		}

		ItemStack item = items.get(0);
		item.setAmount(1);
		int count = 0;

		for (int i = 0; i < itemNum; i++) {
			sInventory.removeItem(item);
			count++;
			if (count >= maxAmount && maxAmount != -1) {
				return;
			}
		}

		if (clearShulkers) {
			Iterator<Material> iterator = MaterialUtils.shulkerTypes.iterator();
			iterateOverShulkerTypes(player, iterator, itemName, maxAmount, shulkerLock, count);
		} else {
			return;
		}

	}

	private static void iterateOverShulkerTypes(Player player, Iterator<Material> iterator, String itemString, int maxAmount, String shulkerLore, int count) {
		Inventory inventory = player.getInventory();

		while(iterator.hasNext()) {
			Material mat = iterator.next();
			if (inventory.contains(mat)) {
				HashMap<Integer, ? extends ItemStack> invMap = inventory.all(mat);
				Iterator<Integer> invIterator = invMap.keySet().iterator();
				while (invIterator.hasNext()) {
					ItemStack shulker = invMap.get(invIterator.next());
					BlockStateMeta shulkerMeta = (BlockStateMeta)shulker.getItemMeta();
					ShulkerBox shulkerBox = (ShulkerBox)shulkerMeta.getBlockState();

					if (!shulker.hasItemMeta() || !shulkerMeta.hasBlockState()) {
						shulkerMeta.setBlockState(shulkerBox);
						shulker.setItemMeta(shulkerMeta);
					}

					if (shulker.getItemMeta().getLore() != null) {
						if (!InventoryUtils.testForItemWithLore(shulker, shulkerLore)) {
							continue;
						}
					} else if (shulker.getItemMeta().getLore() == null) {
						if (!shulkerLore.isEmpty()) {
							continue;
						}
					}

					//Refactor this later
					Inventory sInventory = shulkerBox.getInventory();
					HashMap<Integer, ItemStack> items = new HashMap<>();
					int itemNum = 0;

					ItemStack[] itemsArray = sInventory.getContents();
					int arrayCount = 0;
					for (int i = 0; i < itemsArray.length; i++) {
						if (itemsArray[i] == null) {
								continue;
						}
						ItemMeta meta = itemsArray[i].getItemMeta();
						if (meta.getLore() == null || meta.getDisplayName() == null) {
							continue;
						} else if (meta.getLore().isEmpty() || meta.getDisplayName().isEmpty()) {
							continue;
						}

						if (InventoryUtils.testForItemWithName(itemsArray[i], itemString)) {
							items.put(arrayCount, itemsArray[i]);
							arrayCount++;
						}
					}

					if (items.size() == 0) {
						continue;
					}

					for (int i = 0; i < items.size(); i++) {
						itemNum += items.get(i).getAmount();
					}

					ItemStack item = items.get(0);
					item.setAmount(1);

					for (int i = 0; i < itemNum; i++) {
						sInventory.removeItem(item);
						count++;
						if (count >= maxAmount && maxAmount != -1) {
							break;
						}
					}
					shulkerMeta.setBlockState(shulkerBox);
					shulker.setItemMeta(shulkerMeta);

					if (count >= maxAmount && maxAmount != -1) {
						break;
					}
				}
				if (count >= maxAmount && maxAmount != -1) {
					break;
				}
			}
		}
	}
}
