package com.playmonumenta.scriptedquests.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestNpc;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public class SelectiveNPCVisibilityHandler extends PacketAdapter implements Listener {

	private final Plugin mPlugin;
	private final ProtocolManager mProtocolManager;

	public SelectiveNPCVisibilityHandler(Plugin plugin, ProtocolManager protocolManager) {
		super(plugin, ListenerPriority.NORMAL,
			PacketType.Play.Server.SPAWN_ENTITY,
			PacketType.Play.Server.SPAWN_ENTITY_LIVING,
			PacketType.Play.Server.ENTITY_DESTROY);
		mPlugin = plugin;
		mProtocolManager = protocolManager;
	}

	/**
	 * Map of player UUID to list of entity IDs that are currently hidden to that player
	 */
	private final Map<UUID, Set<Integer>> mHiddenEntities = new HashMap<>();

	/**
	 * Set of entity types where an NPC with a visibility prerequisite exists. Entities whose types are not in this list are always visible and can thus skip being processed.
	 */
	private final EnumSet<EntityType> mEnabledEntityTypes = EnumSet.noneOf(EntityType.class);

	private @Nullable BukkitTask mDespawnTask = null;
	private @Nullable BukkitTask mSpawnTask = null;

	public void reload() {
		mEnabledEntityTypes.clear();
		mPlugin.mNpcManager.getNpcsStream().filter(QuestNpc::hasVisibilityPrerequisites).map(QuestNpc::getEntityType).forEach(mEnabledEntityTypes::add);

		mProtocolManager.removePacketListener(this);
		PlayerChangedWorldEvent.getHandlerList().unregister(this);
		if (!mEnabledEntityTypes.isEmpty()) {
			mProtocolManager.addPacketListener(this);
			Bukkit.getPluginManager().registerEvents(this, plugin);
			if (mDespawnTask == null) {
				mDespawnTask = Bukkit.getScheduler().runTaskTimer(plugin, this::repeatedDespawnCheck, 10, 10);
			}
			if (mSpawnTask == null) {
				mSpawnTask = Bukkit.getScheduler().runTaskTimer(plugin, this::repeatedSpawnCheck, 40, 40);
			}
		} else {
			if (mDespawnTask != null) {
				mDespawnTask.cancel();
				mDespawnTask = null;
			}
			if (mSpawnTask != null) {
				mSpawnTask.cancel();
				mSpawnTask = null;
			}
		}
	}

	@Override
	public void onPacketSending(PacketEvent event) {

		PacketContainer packet = event.getPacket();
		Player player = event.getPlayer();

		if (packet.getType().equals(PacketType.Play.Server.ENTITY_DESTROY)) {
			Set<Integer> hiddenEntities = mHiddenEntities.get(player.getUniqueId());
			if (hiddenEntities != null) {
				if (packet.getIntegerArrays().size() > 0) { // 1.16
					for (int id : packet.getIntegerArrays().read(0)) {
						hiddenEntities.remove(id);
					}
				} else { // 1.18
					for (Integer id : packet.getIntLists().read(0)) {
						hiddenEntities.remove(id);
					}
				}
			}
			return;
		}

		// all used packets have the entity id as first integer (except destroy which is handled above)
		Entity entity = packet.getEntityModifier(event).read(0);

		if (!mEnabledEntityTypes.contains(entity.getType())) {
			return;
		}

		if (isVisible(entity, player)) {
			Set<Integer> hiddenEntities = mHiddenEntities.get(player.getUniqueId());
			if (hiddenEntities != null && hiddenEntities.remove(entity.getEntityId())
				    && !packet.getType().equals(PacketType.Play.Server.SPAWN_ENTITY)
				    && !packet.getType().equals(PacketType.Play.Server.SPAWN_ENTITY_LIVING)) {
				sendEntitySpawnPackets(entity, player);
			}
		} else {
			event.setCancelled(true);
			boolean newlyHidden = mHiddenEntities.computeIfAbsent(player.getUniqueId(), uuid -> new HashSet<>()).add(entity.getEntityId());
			if (newlyHidden) {
				sendEntityDestroyPacket(entity, player);
			}
		}
	}

	private boolean isVisible(Entity entity, Player player) {
		if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
			return true;
		}
		QuestNpc npc = mPlugin.mNpcManager.getInteractNPC(entity);
		return npc == null || npc.isVisibleToPlayer(player, entity);
	}

	/**
	 * Periodically checks if hidden entities near the player should still be invisible, and sends a spawn packet if not.
	 */
	private void repeatedSpawnCheck() {
		for (Iterator<Map.Entry<UUID, Set<Integer>>> playerIter = mHiddenEntities.entrySet().iterator(); playerIter.hasNext(); ) {
			Map.Entry<UUID, Set<Integer>> entry = playerIter.next();
			Player player = Bukkit.getPlayer(entry.getKey());
			if (player == null || !player.isOnline()) {
				playerIter.remove();
				continue;
			}
			for (Iterator<Integer> entityIter = entry.getValue().iterator(); entityIter.hasNext(); ) {
				Integer entityId = entityIter.next();
				Entity entity = mProtocolManager.getEntityFromID(player.getWorld(), entityId);
				if (entity == null || !entity.isValid() || !mProtocolManager.getEntityTrackers(entity).contains(player)) {
					entityIter.remove();
					continue;
				}
				if (isVisible(entity, player)) {
					entityIter.remove();
					sendEntitySpawnPackets(entity, player);
				}
			}
		}
	}

	/**
	 * Periodically checks if visible entities near the player should still be visible, and sends a despawn packet if not.
	 * This has to loop over a lot of entities so is executed less often than {@link #repeatedSpawnCheck()}.
	 */
	private void repeatedDespawnCheck() {
		for (World world : Bukkit.getWorlds()) {
			if (world.getPlayers().isEmpty()) {
				continue;
			}
			for (LivingEntity entity : world.getLivingEntities()) {
				if (!mEnabledEntityTypes.contains(entity.getType())) {
					continue;
				}
				for (Player player : mProtocolManager.getEntityTrackers(entity)) {
					Set<Integer> hiddenEntities = mHiddenEntities.get(player.getUniqueId());
					if (hiddenEntities != null && hiddenEntities.contains(entity.getEntityId())) {
						continue;
					}
					if (!isVisible(entity, player)) {
						mHiddenEntities.computeIfAbsent(player.getUniqueId(), uuid -> new HashSet<>()).add(entity.getEntityId());
						sendEntityDestroyPacket(entity, player);
					}
				}
			}
		}
	}

	private void sendEntitySpawnPackets(Entity entity, Player player) {
		mProtocolManager.updateEntity(entity, List.of(player));
	}

	private void sendEntityDestroyPacket(Entity entity, Player player) {
		PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
		if (packet.getIntegerArrays().size() > 0) { // 1.16
			packet.getIntegerArrays().write(0, new int[] {entity.getEntityId()});
		} else { // 1.18
			packet.getIntLists().write(0, List.of(entity.getEntityId()));
		}
		try {
			mProtocolManager.sendServerPacket(player, packet, false);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerChangedWorldEvent(PlayerChangedWorldEvent event) {
		mHiddenEntities.remove(event.getPlayer().getUniqueId());
	}

}
