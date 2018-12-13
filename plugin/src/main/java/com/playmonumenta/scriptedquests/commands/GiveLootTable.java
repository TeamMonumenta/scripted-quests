package com.playmonumenta.scriptedquests.commands;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.utils.InventoryUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.TextArgument;

public class GiveLootTable {
	@SuppressWarnings("unchecked")
	public static void register(Random random) {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("players", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		arguments.put("lootTablePath", new TextArgument());

		CommandAPI.getInstance().register("giveloottable",
		                                  CommandPermission.fromString("scriptedquests.giveloottable"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      String lootPath = (String)args[1];
		                                      for (Player player : (Collection<Player>)args[0]) {
		                                          try {
		                                              InventoryUtils.giveLootTableContents(player, lootPath, random);
		                                          } catch (Exception e) {
		                                              player.sendMessage(ChatColor.RED + "BUG! Server failed to give you loot from the table '" + lootPath + "'");
		                                              player.sendMessage(ChatColor.RED + "Please hover over the following message, take a screenshot, and report this to a moderator");
		                                              MessagingUtils.sendStackTrace(player, e);
		                                          }
		                                      }
		                                  }
		);
	}
}
