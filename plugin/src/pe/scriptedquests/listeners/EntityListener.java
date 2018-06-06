package pe.scriptedquests.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import pe.scriptedquests.Plugin;
import pe.scriptedquests.quests.QuestNpc;

public class EntityListener implements Listener {
	Plugin mPlugin;

	public EntityListener(Plugin plugin) {
		mPlugin = plugin;
	}

	//  An Entity hit another Entity.
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
					                                  damagee.getType(), npc);
				}
			}
		}
	}
}
