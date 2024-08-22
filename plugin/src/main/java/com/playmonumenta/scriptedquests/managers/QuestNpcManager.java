package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.QuestNpc;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public class QuestNpcManager implements Reloadable {
	private final Map<EntityType, Map<String, List<QuestNpc>>> mNpcs = new HashMap<>();

	public void reload(Plugin plugin, @Nullable CommandSender sender) {
		mNpcs.clear();

		QuestUtils.loadScriptedQuests(plugin, "npcs", sender, (object) -> {
			// Load this file into a QuestNpc object
			QuestNpc npc = new QuestNpc(object);

			mNpcs.computeIfAbsent(npc.getEntityType(), key -> new HashMap<>())
				.computeIfAbsent(npc.getNpcName(), key -> new ArrayList<>())
				.add(npc);

			return npc.getNpcName() + ":" + npc.getComponents().size();
		});
	}

	public boolean isQuestNPC(Entity entity) {
		return isQuestNPC(entity.getCustomName(), entity.getType());
	}

	public boolean isQuestNPC(String npcName, EntityType entityType) {
		List<QuestNpc> npcList = getInteractNPC(npcName, entityType);
		return npcList != null && !npcList.isEmpty();
	}

	public @Nullable List<QuestNpc> getInteractNPC(Entity entity) {
		return getInteractNPC(entity.getCustomName(), entity.getType());
	}

	public @Nullable List<QuestNpc> getInteractNPC(String npcName, EntityType entityType) {
		// Only entities with custom names
		if (npcName == null || npcName.isEmpty()) {
			return null;
		}

		// Return the NPC if we have an NPC with that name
		Map<String, List<QuestNpc>> entityNpcMap = mNpcs.get(entityType);
		if (entityNpcMap == null) {
			return null;
		} else {
			return entityNpcMap.get(QuestNpc.squashNpcName(npcName));
		}
	}

	public boolean interactEvent(QuestContext context, String npcName, EntityType entityType, boolean force) {
		List<QuestNpc> npc = getInteractNPC(npcName, entityType);
		if (npc != null) {
			return interactEvent(context, npcName, entityType, npc, force);
		}
		return false;
	}

	public boolean interactEvent(QuestContext context, String npcName, EntityType entityType, List<QuestNpc> npcFiles, boolean force) {
		// Only one interaction per player per tick
		if (!force && !MetadataUtils.checkOnceThisTick(context.getPlugin(), context.getPlayer(), "ScriptedQuestsNPCInteractNonce")) {
			return false;
		}

		// Check if race allows this
		if (!context.getPlugin().mRaceManager.isNotRacingOrAllowsNpcInteraction(context.getPlayer())) {
			return false;
		}

		if (npcFiles != null) {
			boolean interactionFound = false;
			String squashedNpcName = QuestNpc.squashNpcName(npcName);
			for (QuestNpc file : npcFiles) {
				interactionFound |= file.interactEvent(context, squashedNpcName, entityType);
			}
			return interactionFound;
		}
		return false;
	}

	public Stream<QuestNpc> getNpcsStream() {
		return mNpcs.values().stream().flatMap(e -> e.values().stream().flatMap(Collection::stream));
	}
}
