package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.trades.NpcTrader;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import java.io.File;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class TradesCommand {

	public static void register() {
		Argument<String> tradeFileArgument
			= new GreedyStringArgument("trades_file")
				  .replaceSuggestions(ArgumentSuggestions.stringCollection(
					  info -> Plugin.getInstance().mTradeManager.getTraders().stream().map(
						  trader -> Plugin.getInstance().getDataFolder().toPath().relativize(trader.getFile().toPath()).toString()).toList()));
		EntitySelectorArgument.OnePlayer playerArg = new EntitySelectorArgument.OnePlayer("player");
		Argument<String> npcArg = new TextArgument("npc")
			.replaceSuggestions(ArgumentSuggestions.stringCollection(info -> Plugin.getInstance().mTradeManager.getTraderNames()
				.stream().map(n -> n.contains(" ") ? '"' + n + '"' : n).toList()));
		Argument<String> titleArg = new GreedyStringArgument("title");

		new CommandAPICommand("sqtrades")
			.withPermission("scriptedquests.trades")
			.withSubcommand(
				new CommandAPICommand("edit")
					.withArguments(tradeFileArgument)
					.executesPlayer((player, args) -> {
						String filePath = args.getByArgument(tradeFileArgument);
						File file = new File(Plugin.getInstance().getDataFolder(), filePath);
						Optional<NpcTrader> optionalTrader = Plugin.getInstance().mTradeManager.getTraders().stream().filter(trader -> trader.getFile().getAbsoluteFile().equals(file.getAbsoluteFile())).findFirst();
						if (optionalTrader.isEmpty()) {
							throw CommandAPI.failWithString("Cannot find the given trader file - try /reloadquests if you just added it");
						}
						NpcTrader trader = optionalTrader.get();
						Plugin.getInstance().mTradeManager.editTrader(trader, player);
					})
			).withSubcommand(
				new CommandAPICommand("show")
					.withArguments(
						playerArg,
						npcArg,
						titleArg
					)
					.executes((sender, args) -> {
						Player player = args.getByArgument(playerArg);
						String npc = args.getByArgument(npcArg);
						Component title = MiniMessage.miniMessage().deserialize(args.getByArgument(titleArg));
						List<NpcTrader> trades = Plugin.getInstance().mTradeManager.getTrades(npc);
						if (trades == null) {
							throw CommandAPI.failWithString("No trader file with name '" + npc + "' found!");
						}
						Plugin.getInstance().mTradeManager.trade(Plugin.getInstance(), trades, null, title, player);
					})
			).register();
	}

}
