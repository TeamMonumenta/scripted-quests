package com.playmonumenta.scriptedquests.adapters;

import com.mojang.brigadier.ParseResults;
import javax.annotation.Nullable;
import org.bukkit.block.CommandBlock;
import org.bukkit.plugin.Plugin;

public interface VersionAdapter {

	void setAutoState(CommandBlock state, boolean auto);

	void removeAllMetadata(Plugin plugin);

	@Nullable ParseResults<?> parseCommand(String command);

}
