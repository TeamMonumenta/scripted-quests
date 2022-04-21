package com.playmonumenta.scriptedquests.quests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.point.Point;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.Nullable;

/*
 * A QuestDeath object holds all the quest components bound together with a particular
 * set of death rules (keep inventory, respawn location, etc.)
 */
public class QuestDeath {
	public static class DeathActions {
		private final QuestActions mActions;
		private final @Nullable QuestPrerequisites mPrerequisites;

		public DeathActions(QuestActions actions, @Nullable QuestPrerequisites prerequisites) {
			mActions = actions;
			mPrerequisites = prerequisites;
		}

		public void doActions(Plugin plugin, Player player) {
			mActions.doActions(new QuestContext(plugin, player, null, false, mPrerequisites, null));
		}
	}

	private @Nullable Point mRespawnPt = null;
	private boolean mKeepInv = false;
	private @Nullable QuestPrerequisites mPrerequisites = null;
	private @Nullable QuestActions mActions = null;

	public QuestDeath(JsonObject object) throws Exception {
		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();
			JsonElement value = ent.getValue();

			switch (key) {
			case "keep_inventory":
				mKeepInv = value.getAsBoolean();
				break;
			case "respawn_location":
				JsonObject coordObj = value.getAsJsonObject();
				// Read x coordinate
				JsonElement xElement = coordObj.get("x");
				if (xElement == null) {
					throw new Exception("Failed to parse location x value!");
				}
				double x = xElement.getAsDouble();

				// Read y coordinate
				JsonElement yElement = coordObj.get("y");
				if (yElement == null) {
					throw new Exception("Failed to parse location y value!");
				}
				double y = yElement.getAsDouble();

				// Read z coordinate
				JsonElement zElement = coordObj.get("z");
				if (zElement == null) {
					throw new Exception("Failed to parse location z value!");
				}
				double z = zElement.getAsDouble();

				mRespawnPt = new Point(x, y, z);

				break;
			case "prerequisites":
				mPrerequisites = new QuestPrerequisites(value);
				break;
			case "actions":
				mActions = new QuestActions("", "", EntityType.VILLAGER, 0, value);
				break;
			default:
				throw new Exception("Unknown death quest key: '" + key + "'");
			}
		}

		if (mRespawnPt == null && mKeepInv == false && mActions == null) {
			throw new Exception("Death quest detected with no observeable effects!");
		}
	}

	/* Returns true if prerequisites match and actions were taken, false otherwise */
	@SuppressWarnings("unchecked")
	public boolean deathEvent(Plugin plugin, PlayerDeathEvent event) {
		Player player = event.getEntity();
		if (mPrerequisites == null || mPrerequisites.prerequisiteMet(new QuestContext(plugin, player, null))) {
			List<DeathActions> actionsList;
			if (player.hasMetadata(Constants.PLAYER_RESPAWN_ACTIONS_METAKEY)) {
				actionsList = (List<DeathActions>) player.getMetadata(Constants.PLAYER_RESPAWN_ACTIONS_METAKEY).get(0).value();
			} else {
				actionsList = new ArrayList<DeathActions>(5);
			}
			if (mActions != null) {
				actionsList.add(new DeathActions(mActions, mPrerequisites));
			}
			player.setMetadata(Constants.PLAYER_RESPAWN_ACTIONS_METAKEY,
				new FixedMetadataValue(plugin, actionsList));

			event.setKeepInventory(mKeepInv);
			event.setKeepLevel(mKeepInv);
			if (mKeepInv) {
				event.setDroppedExp(0);
				event.getDrops().clear();
			}

			if (mRespawnPt != null) {
				/*
				 * Attach the respawn point to the player so when they respawn
				 * they can be sent there
				 */
				player.setMetadata(Constants.PLAYER_RESPAWN_POINT_METAKEY,
				                   new FixedMetadataValue(plugin, mRespawnPt));
			}
			return true;
		}
		return false;
	}
}
