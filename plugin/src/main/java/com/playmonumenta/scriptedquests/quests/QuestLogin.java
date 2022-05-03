package com.playmonumenta.scriptedquests.quests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import java.util.Map.Entry;
import java.util.Set;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;

/*
 * A QuestLogin object holds all the quest components bound together with a particular
 * set of login rules (respawn location, etc.)
 */
public class QuestLogin {
	private @Nullable QuestPrerequisites mPrerequisites = null;
	private long mMinLogoutTime = 0;
	private final QuestActions mActions;

	public QuestLogin(JsonObject object) throws Exception {
		QuestActions actions = null;

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();
			JsonElement value = ent.getValue();

			switch (key) {
			case "min_time_logged_out":
				mMinLogoutTime = 1000 * value.getAsLong();
				break;
			case "prerequisites":
				mPrerequisites = new QuestPrerequisites(value);
				break;
			case "actions":
				actions = new QuestActions("", "", EntityType.VILLAGER, 0, value);
				break;
			default:
				throw new Exception("Unknown login quest key: '" + key + "'");
			}
		}

		if (actions == null) {
			throw new Exception("Login quest file has no actions");
		}
		mActions = actions;
	}

	/* Returns true if prerequisites match and actions were taken, false otherwise */
	public boolean loginEvent(Plugin plugin, PlayerJoinEvent event) {
		Player player = event.getPlayer();
		long timeLoggedOut = System.currentTimeMillis() - player.getLastPlayed();

		if (timeLoggedOut < mMinLogoutTime) {
			return false;
		}

		QuestContext context = new QuestContext(plugin, player, null);
		if (mPrerequisites == null || mPrerequisites.prerequisiteMet(context)) {
			mActions.doActions(context.withPrerequisites(mPrerequisites));
			return true;
		}
		return false;
	}
}
