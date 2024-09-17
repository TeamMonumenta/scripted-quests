package com.playmonumenta.scriptedquests.editor;

import com.playmonumenta.scriptedquests.Plugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.metadata.FixedMetadataValue;

public class BookEditor implements Listener {

	private static Map<UUID, Consumer<BookMeta>> listeners = new ConcurrentHashMap<>(); // keep track of who wants the book event

	public static final String BOOK_EDITING_TAG = "ScriptedQuestsBookEditor";


	@EventHandler
	public void onBookSigning(PlayerEditBookEvent e) {
		if (!e.isSigning() || !e.getPlayer().hasMetadata(BOOK_EDITING_TAG)) {
			return;
		}
		final var meta = e.getNewBookMeta();
		final var listener = listeners.remove(e.getPlayer().getUniqueId());
		listener.accept(meta); // notify the listener
		e.getPlayer().removeMetadata(BOOK_EDITING_TAG, Plugin.getInstance());
		e.setCancelled(true);
		Plugin.getInstance().getServer().getScheduler().runTask(Plugin.getInstance(), () -> {
			e.getPlayer().getInventory().setItem(EquipmentSlot.HAND, null);
			e.getPlayer().updateInventory();
		});
	}

	/** TODO better documentation
	 *
	 * @param player The player
	 * @param listener The function that wants the BookMeta
	 *
	 */
	public void addListener(Player player, Consumer<BookMeta> listener) {
		listeners.put(player.getUniqueId(), listener);
		player.setMetadata(BOOK_EDITING_TAG, new FixedMetadataValue(Plugin.getInstance(), true));
	}




}
