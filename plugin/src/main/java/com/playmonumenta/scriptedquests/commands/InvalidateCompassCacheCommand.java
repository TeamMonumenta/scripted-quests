package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import java.util.Collection;
import org.bukkit.entity.Player;

public class InvalidateCompassCacheCommand {
	public static void register(Plugin plugin) {
		EntitySelectorArgument.ManyPlayers playersArg = new EntitySelectorArgument.ManyPlayers("players");
		new CommandAPICommand("invalidatecompasscache")
			.withPermission("scriptedquests.invalidatecompasscache")
			.withArguments(playersArg)
			.executes((sender, args) -> {
				@SuppressWarnings("unchecked")
				Collection<Player> players = args.getByArgument(playersArg);
				for (Player player : players) {
					plugin.mQuestCompassManager.invalidateCache(player);
				}
			}).register();
	}
}
