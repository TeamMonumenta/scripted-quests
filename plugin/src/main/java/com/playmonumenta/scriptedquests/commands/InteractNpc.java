package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestContext;
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
		EntitySelectorArgument.ManyPlayers playersArg = new EntitySelectorArgument.ManyPlayers("players");
		TextArgument nameArg = new TextArgument("npcName");
		EntityTypeArgument typeArg = new EntityTypeArgument("npcType");
		EntitySelectorArgument.OneEntity npcArg = new EntitySelectorArgument.OneEntity("npc");

		new CommandAPICommand("interactnpc")
			.withPermission(CommandPermission.fromString("scriptedquests.interactnpc"))
			.withArguments(playersArg)
			.withArguments(nameArg)
			.withOptionalArguments(typeArg)
			.executes((sender, args) -> {
				Collection<Player> targets = args.getByArgument(playersArg);
				String npcName = args.getByArgument(nameArg);
				EntityType npcType = args.getByArgumentOrDefault(typeArg, EntityType.VILLAGER);
				interact(plugin, sender, targets, npcName, npcType);
			})
			.register();

		new CommandAPICommand("interactnpc")
			.withPermission(CommandPermission.fromString("scriptedquests.interactnpc"))
			.withArguments(playersArg)
			.withArguments(npcArg)
			.executes((sender, args) -> {
				Collection<Player> targets = args.getByArgument(playersArg);
				Entity npc = args.getByArgument(npcArg);
				interact(plugin, sender, targets, npc);
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
