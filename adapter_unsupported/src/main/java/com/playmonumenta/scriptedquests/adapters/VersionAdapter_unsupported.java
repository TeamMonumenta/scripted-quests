package com.playmonumenta.scriptedquests.adapters;

import com.mojang.brigadier.ParseResults;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import org.bukkit.block.CommandBlock;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class VersionAdapter_unsupported implements VersionAdapter {

	@Override
	public void setAutoState(CommandBlock state, boolean auto) {

	}

	public void removeAllMetadata(Plugin plugin) {

	}

	public @Nullable ParseResults<?> parseCommand(String command) {
		return null;
	}

	@Override
	public Component resolveComponents(Component component, Player player) {
		return component;
	}

}
