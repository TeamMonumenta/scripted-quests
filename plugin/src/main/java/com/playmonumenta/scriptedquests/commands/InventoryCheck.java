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
import io.github.jorelali.commandapi.api.arguments.TextArgument;

public class InventoryCheck {
	@SuppressWarnings("unchecked")
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("scriptedquests.inventorycheck");
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		String[] aliases = {"icheck"};

		arguments.put("players", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		arguments.put("name", new TextArgument());

		CommandAPI.getInstance().register("inventorycheck", perms, aliases, arguments, (sender, args) -> {
			return checkInventory((Collection<Player>)args[0], (String)args[1], false, "");
		});

		arguments.put("checkShulkers", new BooleanArgument());

		CommandAPI.getInstance().register("inventorycheck", perms, aliases, arguments, (sender, args) -> {
			return checkInventory((Collection<Player>)args[0], (String)args[1], (boolean)args[2], "");
		});

		arguments.put("shulkerLore", new TextArgument());

		CommandAPI.getInstance().register("inventorycheck", perms, aliases, arguments, (sender, args) -> {
			return checkInventory((Collection<Player>)args[0], (String)args[1], (boolean)args[2], (String)args[3]);
		});
	}

	private static int checkInventory(Collection<Player> players, String itemName, boolean checkShulkers, String shulkerLore) {
		int ret = 0;
		for (Player player : players) {
			ret += checkInventoryLogic(player, itemName, checkShulkers, shulkerLore);
		}
		return ret;
	}

	private static int checkInventoryLogic(Player player, String itemName, boolean checkShulkers, String shulkerLore) {

		Inventory inventory = player.getInventory();
		HashMap<Integer, ItemStack> items = new HashMap<>();
		ItemStack[] itemsArray = inventory.getContents();
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

		int scoreValue = 0;

		if (items.size() == 0) {
			if (checkShulkers) {
				Iterator<Material> iterator = MaterialUtils.shulkerTypes.iterator();
				return checkShulkerLogic(iterator, player, itemName, shulkerLore, scoreValue);
			} else {
				return 0;
			}
		}

		for (int i = 0; i < items.size(); i++) {
			scoreValue += items.get(i).getAmount();
		}

		if (checkShulkers) {
			Iterator<Material> iterator = MaterialUtils.shulkerTypes.iterator();
			return checkShulkerLogic(iterator, player, itemName, shulkerLore, scoreValue);
		} else {
			return scoreValue;
		}
	}

	private static int checkShulkerLogic(Iterator<Material> iterator, Player player, String itemName, String shulkerLore, int scoreValue) {
		Inventory inventory = player.getInventory();

		while (iterator.hasNext()) {
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
						continue;
					}

					for (int i = 0; i < items.size(); i++) {
						scoreValue += items.get(i).getAmount();
					}
				}
			}
		}
		return scoreValue;
	}
}
