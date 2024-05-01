package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.utils.InventoryUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LootTableArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import java.util.Collection;
import java.util.Random;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.loot.LootTable;

public class GiveLootTable {
	@SuppressWarnings("unchecked")
	public static void register(Random random) {
		CommandPermission perms = CommandPermission.fromString("scriptedquests.giveloottable");

		EntitySelectorArgument.ManyPlayers playersArg = new EntitySelectorArgument.ManyPlayers("players");
		LootTableArgument lootTableArg = new LootTableArgument("loot_table");
		TextArgument lootTableTextArg = new TextArgument("lootTablePath");
		IntegerArgument countArg = new IntegerArgument("count", 1, 1000);
		EntitySelectorArgument.OneEntity countItemsArg = new EntitySelectorArgument.OneEntity("countItems");

		new CommandAPICommand("giveloottable")
			.withPermission(perms)
			.withArguments(playersArg)
			.withArguments(lootTableArg)
			.executes((sender, args) -> {
				giveLoot(args.getByArgument(playersArg), args.getByArgument(lootTableArg), 1, random);
			})
			.register();

		new CommandAPICommand("giveloottable")
			.withPermission(perms)
			.withArguments(playersArg)
			.withArguments(lootTableTextArg)
			.executes((sender, args) -> {
				giveLoot(args.getByArgument(playersArg), args.getByArgument(lootTableTextArg), 1, random);
			})
			.register();

		new CommandAPICommand("giveloottable")
			.withPermission(perms)
			.withArguments(playersArg)
			.withArguments(lootTableArg)
			.withArguments(countArg)
			.executes((sender, args) -> {
				giveLoot(args.getByArgument(playersArg), args.getByArgument(lootTableArg), args.getByArgument(countArg), random);
			})
			.register();

		new CommandAPICommand("giveloottable")
			.withPermission(perms)
			.withArguments(playersArg)
			.withArguments(lootTableTextArg)
			.withArguments(countArg)
			.executes((sender, args) -> {
				giveLoot(args.getByArgument(playersArg), args.getByArgument(lootTableTextArg), args.getByArgument(countArg), random);
			})
			.register();

		/* Rather than take a specified count, take an item entity - and give loot from the loot table once for each item in the stack */
		new CommandAPICommand("giveloottable")
			.withPermission(perms)
			.withArguments(playersArg)
			.withArguments(lootTableArg)
			.withArguments(countItemsArg)
			.executes((sender, args) -> {
				giveLootCountFromItemEntity(args.getByArgument(playersArg), args.getByArgument(lootTableArg), args.getByArgument(countItemsArg), random);
			})
			.register();

		new CommandAPICommand("giveloottable")
			.withPermission(perms)
			.withArguments(playersArg)
			.withArguments(lootTableTextArg)
			.withArguments(countItemsArg)
			.executes((sender, args) -> {
				giveLootCountFromItemEntity(args.getByArgument(playersArg), args.getByArgument(lootTableTextArg),  args.getByArgument(countItemsArg), random);
			})
			.register();
	}

	private static void giveLootCountFromItemEntity(Collection<Player> players, String lootPath, Entity entity, Random random) {
		if (entity instanceof Item itemEntity) {
			giveLoot(players, lootPath, itemEntity.getItemStack().getAmount(), random);
		} else {
			for (Player player : players) {
				player.sendMessage(ChatColor.RED + "BUG! Server tried to give you loot based on an entity that wasn't an item from the table '" + lootPath + "'");
				player.sendMessage(ChatColor.RED + "Please take a screenshot and report this bug");
			}
		}
	}

	private static void giveLootCountFromItemEntity(Collection<Player> players, LootTable lootTable, Entity entity, Random random) {
		if (entity instanceof Item itemEntity) {
			giveLoot(players, lootTable, itemEntity.getItemStack().getAmount(), random);
		} else {
			for (Player player : players) {
				player.sendMessage(ChatColor.RED + "BUG! Server tried to give you loot based on an entity that wasn't an item from the table '" + lootTable.getKey() + "'");
				player.sendMessage(ChatColor.RED + "Please take a screenshot and report this bug");
			}
		}
	}

	private static void giveLoot(Collection<Player> players, String lootPath, int count, Random random) {
		LootTable lootTable;
		try {
			lootTable = InventoryUtils.getLootTable(lootPath);
		} catch (Exception e) {
			for (Player player : players) {
				player.sendMessage(ChatColor.RED + "BUG! Server failed to give you loot from the table '" + lootPath + "'");
				player.sendMessage(ChatColor.RED + "Please hover over the following message, take a screenshot, and report this to a moderator");
				MessagingUtils.sendStackTrace(player, e);
			}
			return;
		}
		giveLoot(players, lootTable, count, random);
	}

	private static void giveLoot(Collection<Player> players, LootTable lootTable, int count, Random random) {
		for (Player player : players) {
			boolean alreadyDone = false;
			try {
				for (int i = 0; i < count; i++) {
					alreadyDone = InventoryUtils.giveLootTableContents(player, lootTable, random, alreadyDone);
				}
			} catch (Exception e) {
				player.sendMessage(ChatColor.RED + "BUG! Server failed to give you loot from the table '" + lootTable.getKey() + "'");
				player.sendMessage(ChatColor.RED + "Please hover over the following message, take a screenshot, and report this to a moderator");
				MessagingUtils.sendStackTrace(player, e);
			}
		}
	}
}
