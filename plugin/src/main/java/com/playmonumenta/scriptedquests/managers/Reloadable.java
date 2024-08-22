package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Plugin;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

public interface Reloadable {


	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	void reload(Plugin plugin, @Nullable  CommandSender sender);
}
