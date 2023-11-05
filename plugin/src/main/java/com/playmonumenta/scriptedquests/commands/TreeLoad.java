package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.zones.ZoneManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import org.bukkit.ChatColor;

public class TreeLoad {
	public static void register(Plugin plugin) {
		new CommandAPICommand("treeload")
			.withPermission(CommandPermission.fromString("scriptedquests.reloadzones"))
			.executes((sender, args) -> {
				sender.sendMessage(ChatColor.GOLD + "Reloading zone tree...");
				ZoneManager.getInstance().treeLoad(sender);
				return 1;
			})
			.register();
	}
}
