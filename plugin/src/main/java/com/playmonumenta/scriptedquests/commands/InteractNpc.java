package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntityTypeArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import java.util.Collection;
import java.util.UUID;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class InteractNpc {
	static final Pattern uuidRegex = Pattern.compile("\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}");

	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		/* First one of these has both required arguments */
		new CommandAPICommand("interactnpc")
			.withPermission(CommandPermission.fromString("scriptedquests.interactnpc"))
			.withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
			.withArguments(new TextArgument("npcName"))
			.withArguments(new EntityTypeArgument("npcType"))
			.executes((sender, args) -> {
				Collection<Player> targets = (Collection<Player>) args[0];
				if (sender instanceof Player player) {
					if (!player.isOp() && (targets.size() > 1 || !targets.contains(player))) {
						CommandAPI.fail("You do not have permission to run this as another player.");
					}
				}
				String npcName = (String) args[1];
				EntityType npcType = (EntityType) args[2];
				interact(plugin, sender, targets, npcName, npcType);
			})
			.register();

		/* Second one accepts a single NPC entity, and goes earlier to take priority over entity names */
		new CommandAPICommand("interactnpc")
			.withPermission(CommandPermission.fromString("scriptedquests.interactnpc"))
			.withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
			.withArguments(new EntitySelectorArgument("npc", EntitySelectorArgument.EntitySelector.ONE_ENTITY))
			.executes((sender, args) -> {
				Collection<Player> targets = (Collection<Player>) args[0];
				if (sender instanceof Player player) {
					if (!player.isOp() && (targets.size() > 1 || !targets.contains(player))) {
						CommandAPI.fail("You do not have permission to run this as another player.");
					}
				}
				Entity npc = (Entity) args[1];
				interact(plugin, sender, targets, npc);
			})
			.register();

		/* Third one just has the npc name with VILLAGER as default */
		new CommandAPICommand("interactnpc")
			.withPermission(CommandPermission.fromString("scriptedquests.interactnpc"))
			.withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
			.withArguments(new TextArgument("npcName"))
			.executes((sender, args) -> {
				Collection<Player> targets = (Collection<Player>) args[0];
				if (sender instanceof Player player) {
					if (!player.isOp() && (targets.size() > 1 || !targets.contains(player))) {
						CommandAPI.fail("You do not have permission to run this as another player.");
					}
				}
				String npcName = (String) args[1];
				interact(plugin, sender, targets, npcName, EntityType.VILLAGER);
			})
			.register();
	}

	private static void interact(Plugin plugin, CommandSender sender, Collection<Player> players,
	                             String npcName, EntityType npcType) {
		if (uuidRegex.matcher(npcName).matches()) {
			UUID npcUuid = UUID.fromString(npcName);
			Entity npc = Bukkit.getEntity(npcUuid);
			if (npc == null) {
				sender.sendMessage(ChatColor.RED + "No NPC with UUID '" + npcName + "'");
			} else {
				interact(plugin, sender, players, npc);
			}
			return;
		}

		if (plugin.mNpcManager != null) {
			QuestContext currentContext = QuestContext.getCurrentContext();
			for (Player player : players) {
				QuestContext context = new QuestContext(plugin, player, null, false, null, currentContext != null ? currentContext.getUsedItem() : null);
				if (!plugin.mNpcManager.interactEvent(context, npcName, npcType, true)) {
					sender.sendMessage(ChatColor.RED + "No interaction available for player '" + player.getName() +
						                   "' and NPC '" + npcName + "'");
				}
			}
		}
	}

	private static void interact(Plugin plugin, CommandSender sender, Collection<Player> players, Entity npc) {
		if (plugin.mNpcManager != null) {
			QuestContext currentContext = QuestContext.getCurrentContext();
			for (Player player : players) {
				QuestContext context = new QuestContext(plugin, player, npc, false, null, currentContext != null ? currentContext.getUsedItem() : null);
				if (!plugin.mNpcManager.interactEvent(context, npc.getCustomName(), npc.getType(), false)) {
					sender.sendMessage(ChatColor.RED + "No interaction available for player '" + player.getName() +
						                   "' and NPC '" + npc.getCustomName() + "'");
				}
			}
		}
	}
}
