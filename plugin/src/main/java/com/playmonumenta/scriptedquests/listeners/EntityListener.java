package com.playmonumenta.scriptedquests.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestNpc;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.PotionSplashEvent;

public class EntityListener implements Listener {
	Plugin mPlugin;

	public EntityListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void EntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
		Entity damagee = event.getEntity();
		Entity damager = event.getDamager();

		if (damager instanceof Player) {
			Player player = (Player)damager;

			QuestNpc npc = mPlugin.mNpcManager.getInteractNPC(damagee.getCustomName(), damagee.getType());
			if (npc != null) {
				/*
				 * This is definitely a quest NPC, even if the player might not be able to interact with it
				 * Cancel all damage done to it
				 */
				event.setCancelled(true);

				/* Only trigger quest interactions via melee attack */
				if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
					mPlugin.mNpcManager.interactEvent(mPlugin, player, damagee.getCustomName(),
					                                  damagee.getType(), npc, false);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void EntityDamageEvent(EntityDamageEvent event) {
		Entity damagee = event.getEntity();

		QuestNpc npc = mPlugin.mNpcManager.getInteractNPC(damagee.getCustomName(), damagee.getType());
		if (npc != null) {
			DamageCause cause = event.getCause();
			if (cause != DamageCause.CUSTOM && cause != DamageCause.VOID) {
				/* This is a quest NPC - refuse to damage it */
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void PotionSplashEvent(PotionSplashEvent event) {
		// Don't apply potion effects to quest entities
		event.getAffectedEntities().removeIf(entity -> mPlugin.mNpcManager.getInteractNPC(entity.getCustomName(), entity.getType()) != null);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void AreaEffectCloudApplyEvent(AreaEffectCloudApplyEvent event) {
		// Don't apply potion effects to quest entities
		event.getAffectedEntities().removeIf(entity -> mPlugin.mNpcManager.getInteractNPC(entity.getCustomName(), entity.getType()) != null);
	}
}
