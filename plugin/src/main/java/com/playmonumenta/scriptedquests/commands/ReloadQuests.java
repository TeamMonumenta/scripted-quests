package com.playmonumenta.scriptedquests.commands;

import com.playmonumenta.scriptedquests.Plugin;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.Nullable;

public class ReloadQuests implements BasicCommand {
	@Override
	public void execute(CommandSourceStack commandSourceStack, String[] args) {
		Entity sender = commandSourceStack.getExecutor();
		if (args.length > 0) {
			sender.sendMessage(Component.text("No parameters are needed for this function!", NamedTextColor.RED));
			return;
		}

		sender.sendMessage(Component.text("Reloading config...", NamedTextColor.GOLD));

		Plugin.getInstance().reloadConfig(sender);
	}

	@Override
	public @Nullable String permission() {
		return "scriptedquests.reloadquests";
	}
}
