package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import me.Novalescent.Constants;
import me.Novalescent.mobs.npcs.RPGNPC;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DialogScrollingText implements DialogBase {

	public class ScrollingTextRunnable extends BukkitRunnable {

		private Plugin mPlugin;
		private int mIndex = -1;
		private int mTimer = 20 * 4;
		private List<String> mText;
		private Player mPlayer;
		private Entity mEntity;
		private QuestActions mActions;
		private QuestPrerequisites mPrerequisites;
		public ScrollingTextRunnable(Plugin plugin, Player player, Entity npcEntity, List<String> text, QuestActions actions, QuestPrerequisites prerequisites) {
			mPlugin = plugin;
			mPlayer = player;
			mEntity = npcEntity;
			mText = text;
			mActions = actions;
			mPrerequisites = prerequisites;
		}

		public void next() {
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
			}

			if (mIndex >= mText.size()) {
				this.cancel();
				mActions.doActions(mPlugin, mPlayer, mEntity, mPrerequisites);
				mPlayer.removeMetadata(com.playmonumenta.scriptedquests.Constants.PLAYER_SCROLLING_DIALOG_METAKEY, mPlugin);
			}

			mTimer = 0;
		}

		@Override
		public void run() {

			if (mTimer >= 20 * 4) {
				next();
			}
			mTimer++;

		}
	}

	private String mDisplayName;
	private ArrayList<String> mText = new ArrayList<String>();
	private QuestActions mActions;

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
	}

	@Override
	public void sendDialog(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		ScrollingTextRunnable runnable = new ScrollingTextRunnable(plugin, player, npcEntity, mText, mActions, prereqs);

		String metakey = com.playmonumenta.scriptedquests.Constants.PLAYER_SCROLLING_DIALOG_METAKEY;

		if (player.hasMetadata(metakey)) {
			ScrollingTextRunnable currentRunnable = (ScrollingTextRunnable) player.getMetadata(metakey).get(0).value();
			currentRunnable.cancel();
		}

		player.setMetadata(metakey, new FixedMetadataValue(plugin, runnable));

		runnable.runTaskTimer(plugin, 0, 1);
	}
}
