package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.point.AreaBounds;
import com.playmonumenta.scriptedquests.point.Point;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

public class DialogClickableTextEntry implements DialogBase {
	public class PlayerClickableTextEntry {
		private QuestPrerequisites mPrerequisites;
		private QuestActions mActions;
		private AreaBounds mValidArea;

		public PlayerClickableTextEntry(QuestPrerequisites prereqs, QuestActions actions,
		                                AreaBounds validArea) {
			mPrerequisites = prereqs;
			mActions = actions;
			mValidArea = validArea;
		}

		public void doActionsIfConditionsMatch(Plugin plugin, Player player) {
			if (mValidArea.within(player.getLocation())
			    && (mPrerequisites == null || mPrerequisites.prerequisiteMet(player))) {
				player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 0.9f);
				mActions.doActions(plugin, player, mPrerequisites);
			}
		}
	}

	private String mText;
	private QuestActions mActions;
	private int mIdx;

	public DialogClickableTextEntry(String npcName, String displayName, EntityType entityType,
	                                JsonElement element, int elementIdx) throws Exception {
		mIdx = elementIdx;

		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("dialog value is not an object!");
		}

		// Read the delay_actions_by_ticks field first, if specified
		JsonElement delayElement = object.get("delay_actions_by_ticks");
		int delayTicks = 0;
		if (delayElement != null) {
			delayTicks = delayElement.getAsInt();
		}

		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("player_text") && !key.equals("actions") && !key.equals("delay_actions_by_ticks")) {
				throw new Exception("Unknown clickable_text key: " + key);
			}

			// All quest_components entries are single JSON things that should be passed
			// to their respective handlers
			JsonElement value = object.get(key);
			if (value == null) {
				throw new Exception("clickable_text value for key '" + key + "' is not parseable!");
			}

			if (key.equals("player_text")) {
				mText = value.getAsString();
				if (mText == null) {
					throw new Exception("clickable_text player_text entry is not a string!");
				}
			} else if (key.equals("actions")) {
				mActions = new QuestActions(npcName, displayName, entityType, delayTicks, value);
			}
		}

		if (mActions == null) {
			throw new Exception("clickable_text value without an action!");
		}
	}


	@SuppressWarnings("unchecked")
	@Override
	public void sendDialog(Plugin plugin, Player player, QuestPrerequisites prereqs) {
		MessagingUtils.sendClickableNPCMessage(plugin, player, mText,
		                                       "/questtrigger " + Integer.toString(mIdx));

		/* Create a new object describing the prereqs/actions/location for this clickable message */
		PlayerClickableTextEntry newEntry = new PlayerClickableTextEntry(prereqs, mActions,
		        new AreaBounds("", new Point(player.getLocation().subtract(4.0, 4.0, 4.0)),
		                       new Point(player.getLocation().add(4.0, 4.0, 4.0))));

		/* Get the list of currently available clickable entries */
		HashMap<Integer, PlayerClickableTextEntry> availTriggers;
		if (player.hasMetadata(Constants.PLAYER_CLICKABLE_DIALOG_METAKEY)) {
			availTriggers = (HashMap<Integer, PlayerClickableTextEntry>)player.getMetadata(
			                    Constants.PLAYER_CLICKABLE_DIALOG_METAKEY).get(0).value();
		} else {
			availTriggers = new HashMap<Integer, PlayerClickableTextEntry>();
		}

		/* Then we add this entry to the end of all available entries */
		availTriggers.put(mIdx, newEntry);

		/* Attach the new list of clickable options to the player */
		player.setMetadata(Constants.PLAYER_CLICKABLE_DIALOG_METAKEY,
		                   new FixedMetadataValue(plugin, availTriggers));
	}
}

