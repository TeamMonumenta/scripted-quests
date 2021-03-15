package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.point.AreaBounds;
import com.playmonumenta.scriptedquests.point.Point;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import me.Novalescent.Constants;
import me.Novalescent.mobs.npcs.RPGNPC;
import me.Novalescent.utils.FormattedMessage;
import me.Novalescent.utils.MessageFormat;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DialogScrollingText implements DialogBase {

	public class ScrollingTextActive {

		private Plugin mPlugin;
		private int mIndex = -1;
		private List<String> mText;
		private Player mPlayer;
		private Entity mEntity;
		private QuestActions mActions;
		private QuestPrerequisites mPrerequisites;
		private final AreaBounds mValidArea;

		public ScrollingTextActive(Plugin plugin, Player player, Entity npcEntity,
								   List<String> text, QuestActions actions, QuestPrerequisites prerequisites, AreaBounds validArea) {
			mPlugin = plugin;
			mPlayer = player;
			mEntity = npcEntity;
			mText = text;
			mActions = actions;
			mPrerequisites = prerequisites;
			mValidArea = validArea;
		}

		public void next() {

			if (!mValidArea.within(mPlayer.getLocation())) {
				mPlayer.playSound(mPlayer.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 0.3f);
				FormattedMessage.sendMessage(mPlayer, MessageFormat.NOTICE, ChatColor.RED + "You moved too far away to hear the dialogue...");
				mPlayer.removeMetadata(com.playmonumenta.scriptedquests.Constants.PLAYER_SCROLLING_DIALOG_METAKEY, mPlugin);
				return;
			} else if (mPrerequisites != null && !mPrerequisites.prerequisiteMet(mPlayer, mEntity)) {
				mPlayer.playSound(mPlayer.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 0.3f);
				FormattedMessage.sendMessage(mPlayer, MessageFormat.NOTICE, ChatColor.RED + "You no longer meet the requirements to listen to this dialogue...");
				mPlayer.removeMetadata(com.playmonumenta.scriptedquests.Constants.PLAYER_SCROLLING_DIALOG_METAKEY, mPlugin);
				return;
			}

			String name = "";
			if (mEntity != null && mEntity.hasMetadata(Constants.NPC_METAKEY)) {
				RPGNPC npc = (RPGNPC) mEntity.getMetadata(Constants.NPC_METAKEY).get(0).value();
				name = ChatColor.stripColor(npc.mNameStand.getCustomName());
			}
			mIndex++;
			if (mIndex < mText.size()) {
				String text = mText.get(mIndex);
				if (!text.trim().isEmpty()) {
					MessagingUtils.sendScrollableNPCMessage(mPlayer, name, text);
				} else {
					next();
					return;
				}
			} else {
				mActions.doActions(mPlugin, mPlayer, mEntity, mPrerequisites);
				mPlayer.removeMetadata(com.playmonumenta.scriptedquests.Constants.PLAYER_SCROLLING_DIALOG_METAKEY, mPlugin);
				return;
			}

		}

	}

	private Double mRadius = 4.0;
	private String mDisplayName;
	private ArrayList<String> mText = new ArrayList<String>();
	private QuestActions mActions;
	public int mClickType = 0;

	public DialogScrollingText(String displayName, JsonElement element)  throws Exception {
		mDisplayName = displayName;
		JsonObject jsonObject = element.getAsJsonObject();
		if (jsonObject == null) {
			throw new Exception("scrolling_text value is not an object!");
		}

		JsonArray array = jsonObject.get("text").getAsJsonArray();
		Iterator<JsonElement> iter = array.iterator();
		while (iter.hasNext()) {
			mText.add(iter.next().getAsString());
		}

		mActions = new QuestActions("", displayName, EntityType.VILLAGER, 0, jsonObject.get("actions"));

		if (jsonObject.has("click_type")) {
			mClickType = jsonObject.get("click_type").getAsInt();
		}
	}

	@Override
	public void sendDialog(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		ScrollingTextActive active = new ScrollingTextActive(plugin, player, npcEntity, mText, mActions, prereqs,
			new AreaBounds("", new Point(player.getLocation().subtract(mRadius, mRadius, mRadius)),
				new Point(player.getLocation().add(mRadius, mRadius, mRadius))));

		String metakey = com.playmonumenta.scriptedquests.Constants.PLAYER_SCROLLING_DIALOG_METAKEY;
		player.setMetadata(metakey, new FixedMetadataValue(plugin, active));

		active.next();
	}
}
