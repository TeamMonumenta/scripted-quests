package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonPrimitive;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;

public class DialogClickableTextEntry implements DialogBase {
	public class PlayerClickableTextEntry {
		private final QuestPrerequisites mPrerequisites;
		private final QuestActions mActions;
		private final AreaBounds mValidArea;
		private final Entity mNpcEntity;

		public PlayerClickableTextEntry(QuestPrerequisites prereqs, QuestActions actions,
		                                Entity npcEntity, AreaBounds validArea) {
			mPrerequisites = prereqs;
			mActions = actions;
			mValidArea = validArea;
			mNpcEntity = npcEntity;
		}

		public void doActionsIfConditionsMatch(Plugin plugin, Player player) {
			if (!mValidArea.within(player.getLocation())) {
				player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 0.3f);
				player.sendMessage(ChatColor.RED + "You moved too far away to be heard");
			} else if (mPrerequisites != null && !mPrerequisites.prerequisiteMet(player, mNpcEntity)) {
				player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 0.3f);
				player.sendMessage(ChatColor.RED + "You no longer meet the requirements for this option");
			} else {
				player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 0.9f);
				mActions.doActions(plugin, player, mNpcEntity, mPrerequisites);
			}
		}
	}

	private String mText;
	private Double mRadius = 4.0;
	private QuestActions mActions;
	private int mIdx;
	private HoverEvent<Component> mHoverEvent = null;

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

			if (!key.equals("player_text") && !key.equals("player_valid_radius") && !key.equals("actions") && !key.equals("delay_actions_by_ticks") && !key.equals("hover_text")) {
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
			} else if (key.equals("player_valid_radius")) {
				mRadius = value.getAsDouble();
			} else if (key.equals("actions")) {
				mActions = new QuestActions(npcName, displayName, entityType, delayTicks, value);
			} else if (key.equals("hover_text")) {
				mHoverEvent = HoverEvent.showText(MessagingUtils.AMPERSAND_SERIALIZER.deserialize(value.getAsString().replace("§", "&")));
			}
		}

		if (mActions == null) {
			throw new Exception("clickable_text value without an action!");
		}
	}

	@SuppressWarnings("unchecked")
	private void setupTriggersEntries(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		PlayerClickableTextEntry newEntry = new PlayerClickableTextEntry(prereqs, mActions, npcEntity,
			new AreaBounds("", new Point(player.getLocation().subtract(mRadius, mRadius, mRadius)),
				new Point(player.getLocation().add(mRadius, mRadius, mRadius))));

		/* Get the list of currently available clickable entries */
		HashMap<Integer, PlayerClickableTextEntry> availTriggers;
		if (player.hasMetadata(Constants.PLAYER_CLICKABLE_DIALOG_METAKEY)) {
			availTriggers = (HashMap<Integer, PlayerClickableTextEntry>)player.getMetadata(
				Constants.PLAYER_CLICKABLE_DIALOG_METAKEY).get(0).value();
		} else {
			availTriggers = new HashMap<>();
		}

		/* Then we add this entry to the end of all available entries */
		availTriggers.put(mIdx, newEntry);

		/* Attach the new list of clickable options to the player */
		player.setMetadata(Constants.PLAYER_CLICKABLE_DIALOG_METAKEY,
			new FixedMetadataValue(plugin, availTriggers));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void sendDialog(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		MessagingUtils.sendClickableNPCMessage(plugin, player, mText, "/questtrigger " + mIdx, mHoverEvent);
		setupTriggersEntries(plugin, player, npcEntity, prereqs);
	}

	@Override
	public JsonElement serialize(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		JsonObject tmp = JsonObjectBuilder.get()
			.add("command", "/questtrigger " + mIdx)
			.add("text", mText)
			.build();
		setupTriggersEntries(plugin, player, npcEntity, prereqs);
		return tmp;
	}
}

