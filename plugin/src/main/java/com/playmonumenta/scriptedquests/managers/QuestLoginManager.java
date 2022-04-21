package com.playmonumenta.scriptedquests.managers;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestLogin;
import com.playmonumenta.scriptedquests.utils.QuestUtils;
import java.util.ArrayList;
import org.bukkit.command.CommandSender;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;

public class QuestLoginManager {
	private final ArrayList<QuestLogin> mLogins = new ArrayList<QuestLogin>();

	/* If sender is non-null, it will be sent debugging information */
	public void reload(Plugin plugin, @Nullable CommandSender sender) {
		mLogins.clear();
		QuestUtils.loadScriptedQuests(plugin, "login", sender, (object) -> {
			mLogins.add(new QuestLogin(object));
			return null;
		});
	}

	public boolean loginEvent(Plugin plugin, PlayerJoinEvent event) {
		boolean success = false;

		/* Try each available login-triggered quest */
		for (QuestLogin login : mLogins) {
			/* Don't stop after the first matching quest */
			if (login.loginEvent(plugin, event)) {
				success = true;
			}
		}

		return success;
	}
}
