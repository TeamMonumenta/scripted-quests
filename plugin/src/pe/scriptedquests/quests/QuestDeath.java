package pe.scriptedquests.quests;

import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.scriptedquests.Constants;
import pe.scriptedquests.Plugin;
import pe.scriptedquests.point.Point;

/*
 * A QuestDeath object holds all the quest components bound together with a particular
 * set of death rules (keep inventory, respawn location, etc.)
 */
public class QuestDeath {
	private Point mRespawnPt = null;
	private boolean mKeepInv = false;
	private QuestPrerequisites mPrerequisites = null;
	private QuestActions mActions = null;

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
				mPrerequisites = new QuestPrerequisites(value,"OP_AND");
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
	public boolean deathEvent(Plugin plugin, PlayerDeathEvent event) {
		Player player = event.getEntity();
		if (mPrerequisites == null || mPrerequisites.prerequisitesMet(player)) {
			mActions.doActions(plugin, player, mPrerequisites);

			event.setKeepInventory(mKeepInv);
			event.setKeepLevel(mKeepInv);
			if (mKeepInv) {
				event.setDroppedExp(0);
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
