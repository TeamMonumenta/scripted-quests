package com.playmonumenta.scriptedquests.managers;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.playmonumenta.scriptedquests.utils.MaterialUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.InteractableEntry;
import com.playmonumenta.scriptedquests.quests.InteractableEntry.InteractType;
import com.playmonumenta.scriptedquests.utils.QuestUtils;

public class InteractableManager {
	private final Map<Material, List<InteractableEntry>> mInteractables = new EnumMap<>(Material.class);

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, CommandSender sender) {
		mInteractables.clear();

		QuestUtils.loadScriptedQuests(plugin, "interactables", sender, (object) -> {
			InteractableEntry interactable = new InteractableEntry(object);

			if (!mInteractables.containsKey(interactable.getMaterial())) {
				mInteractables.put(interactable.getMaterial(), new ArrayList<>());
			}
			mInteractables.get(interactable.getMaterial()).add(interactable);

			return interactable.getMaterial() + ":" + interactable.getComponents().size();
		});
	}

	/**
	 * interactEvent should be called by listeners whenever a player left or right clicks air or a block
	 * @param plugin The plugin calling the event
	 * @param player The player who clicked
	 * @param item   The item the player clicked with
	 * @param block  The block the player clicked. Null if clicked air.
	 * @param action The click type, determines if it was a left/right click, and if it clicked air or a block.
	 * @return True if the PlayerInteractEvent that called this method should be cancelled.
	 *         Some interactables do cancel the event, others do not.
	 */
	public boolean interactEvent(Plugin plugin, Player player, ItemStack item, Block block, Action action) {
		boolean cancelEvent = false;
		if (item != null && mInteractables.containsKey(item.getType())) {
			InteractType interact;
			switch (action) {
				case RIGHT_CLICK_AIR:
					interact = InteractType.RIGHT_CLICK_AIR;
					break;
				case RIGHT_CLICK_BLOCK:
					interact = (block != null && MaterialUtils.isInteractableBlock(block.getType())) ? InteractType.RIGHT_CLICK_FUNCTIONAL : InteractType.RIGHT_CLICK_BLOCK;
					break;
				case LEFT_CLICK_AIR:
					interact = InteractType.LEFT_CLICK_AIR;
					break;
				case LEFT_CLICK_BLOCK:
					interact = InteractType.LEFT_CLICK_BLOCK;
					break;
				default:
					return false;
			}
			for (InteractableEntry entry : mInteractables.get(item.getType())) {
				if (entry.interactEvent(plugin, player, null, interact)) {
					cancelEvent = true;
				}
			}
		}
		return cancelEvent;
	}


	/**
	 * interactEvent should be called by listeners whenever a player left or right clicks air or a block
	 * @param plugin The plugin calling the event
	 * @param player The player who clicked
	 * @param item   The item the player clicked with
	 * @param target The entity the player clicked.
	 * @return True if the PlayerInteractEntityEvent that called this method should be cancelled.
	 *         Some interactables do cancel the event, others do not.
	 */
	public boolean interactEntityEvent(Plugin plugin, Player player, ItemStack item, Entity target) {
		boolean cancelEvent = false;
		if (mInteractables.containsKey(item.getType())) {
			for (InteractableEntry entry : mInteractables.get(item.getType())) {
				if (entry.interactEvent(plugin, player, target, InteractType.RIGHT_CLICK_ENTITY)) {
					cancelEvent = true;
				}
			}
		}
		return cancelEvent;
	}

	/**
	 * interactEvent should be called by listeners whenever a player left or right clicks air or a block
	 * @param plugin The plugin calling the event
	 * @param player The player who clicked
	 * @param item   The item the player clicked with
	 * @param target The entity the player clicked.
	 * @return True if the EntityDamageByEntityEvent that called this method should be cancelled.
	 *         Some interactables do cancel the event, others do not.
	 */
	public boolean attackEntityEvent(Plugin plugin, Player player, ItemStack item, Entity target) {
		boolean cancelEvent = false;
		if (mInteractables.containsKey(item.getType())) {
			for (InteractableEntry entry : mInteractables.get(item.getType())) {
				if (entry.interactEvent(plugin, player, target, InteractType.LEFT_CLICK_ENTITY)) {
					cancelEvent = true;
				}
			}
		}
		return cancelEvent;
	}
}
