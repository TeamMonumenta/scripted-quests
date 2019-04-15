package com.playmonumenta.scriptedquests.quests;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.point.Point;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;

/*
 * A QuestLogout object holds all the quest components bound together with a particular
 * set of logout rules (respawn location, etc.)
 */
public class QuestLogout {
	public class LogoutActions {
		private final QuestActions mActions;
		private final QuestPrerequisites mPrerequisites;

		public LogoutActions(QuestActions actions, QuestPrerequisites prerequisites) {
			mActions = actions;
			mPrerequisites = prerequisites;
		}

		public void doActions(Plugin plugin, Player player) {
			mActions.doActions(plugin, player, null, mPrerequisites);
		}
	}

	private Point mRespawnPt = null;
	private QuestPrerequisites mPrerequisites = null;
	private QuestActions mActions = null;

	public QuestLogout(JsonObject object) throws Exception {
		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();
			JsonElement value = ent.getValue();

			switch (key) {
			case "respawn_location":
				JsonObject coordObj = value.getAsJsonObject();
				// Read x coordinate
				JsonElement xElement = coordObj.get("x");
				int x = 0;
				if (xElement == null) {
					throw new Exception("Failed to parse location x value!");
				}
				x = xElement.getAsInt();

				// Read y coordinate
				JsonElement yElement = coordObj.get("y");
				int y = 0;
				if (yElement == null) {
					throw new Exception("Failed to parse location y value!");
				}
				y = yElement.getAsInt();

				// Read z coordinate
				JsonElement zElement = coordObj.get("z");
				int z = 0;
				if (zElement == null) {
					throw new Exception("Failed to parse location z value!");
				}
				z = zElement.getAsInt();

				mRespawnPt = new Point(x, y, z);

				break;
			case "prerequisites":
				mPrerequisites = new QuestPrerequisites(value);
				break;
			case "actions":
				mActions = new QuestActions("", "", EntityType.VILLAGER, 0, value);
				break;
			default:
				throw new Exception("Unknown logout quest key: '" + key + "'");
			}
		}

		if (mRespawnPt == null && mActions == null) {
			throw new Exception("Logout quest detected with no observeable effects!");
		}
	}

	/* Returns true if prerequisites match and actions were taken, false otherwise */
	@SuppressWarnings("unchecked")
	public boolean logoutEvent(Plugin plugin, PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if (mPrerequisites == null || mPrerequisites.prerequisiteMet(player, null)) {
			mActions.doActions(plugin, player, null, mPrerequisites);

			if (mRespawnPt != null) {
				player.teleport(mRespawnPt.toLocation(player.getWorld()));
			}
			return true;
		}
		return false;
	}
}
