package pe.scriptedquests.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import pe.scriptedquests.Plugin;

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

			if (mPlugin.mNpcManager.interactEvent(mPlugin, player, damagee.getCustomName(),
			                                      damagee.getType())) {
				// This resulted in a quest interaction - cancel the damage event
				event.setCancelled(true);
				return;
			}
		}
	}
}
