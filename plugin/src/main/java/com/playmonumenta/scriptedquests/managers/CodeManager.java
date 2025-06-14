package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.CodeEntry;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class CodeManager {
	private final Map<String, CodeEntry> mCodes = new HashMap<String, CodeEntry>();

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, @Nullable CommandSender sender) {

		mCodes.clear();

		QuestUtils.loadScriptedQuests(plugin, "codes", sender, (object) -> {
			// Load this file into a NpcTrader object
			CodeEntry code = new CodeEntry(object);

			if (mCodes.containsKey(code.getSeed())) {
				throw new Exception(code.getSeed() + "' already exists!");
			}

			mCodes.put(code.getSeed(), code);

			return code.getSeed() + ":" + Integer.toString(code.getComponents().size());
		});
	}

	public void playerEnteredCodeEvent(Plugin plugin, Player player, String code) {
		boolean success = false;
		for (CodeEntry entry : mCodes.values()) {
			if (entry.doActionsIfCodeMatches(plugin, player, code)) {
				success = true;
			}
		}

		if (!success) {
			player.sendMessage(Component.text("The code ", NamedTextColor.RED)
				.append(Component.text(code, NamedTextColor.GOLD))
				.append(Component.text(" is not applicable to you!", NamedTextColor.RED)));
		}
	}

	public void generateCodeForPlayer(Player player, String seed) {
		final String code;
		final CodeEntry entry = mCodes.get(seed);
		if (entry == null) {
			player.sendMessage(Component.text("Warning: This server does not support this code seed", NamedTextColor.RED));
			code = CodeEntry.getCodeForPlayer(player, seed);
		} else {
			code = entry.getCodeForPlayer(player);
		}

		player.sendMessage(Component.text(" /code " + code, NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
	}
}


