package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.point.AreaBounds;
import com.playmonumenta.scriptedquests.point.Point;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionBase;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionNested;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionQuestMarker;
import com.playmonumenta.scriptedquests.quests.components.actions.quest.ActionQuest;
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class DialogScrollingText extends ActionQuestMarker implements DialogBase, ActionNested {
	private Double mRadius = 4.0;
	private String mDisplayName;
	private ArrayList<ScrollingTextEntry> mText = new ArrayList<>();
	private QuestActions mActions;
	public int mClickType = 0;
	private boolean mRaw = false;
	private boolean mAutoScroll = false;
	private boolean mTriggerActionsOnLastDialog = false;
	private final ActionNested mParent;

	public class ScrollingTextEntry {

		private String mName;
		private boolean mRaw;
		private String mText;
		public ScrollingTextEntry(String name, JsonObject object) {
			mName = object.get("speaker").getAsString();
			if (mName.trim().isEmpty()) {
				mName = name;
			}
			mRaw = object.get("raw").getAsBoolean();
			mText = object.get("text").getAsString();
		}

		public ScrollingTextEntry(String name, String string) {
			mName = name;
			mText = string;
		}

		public String getSpeaker() {
			return mName;
		}

		public String getText() {
			return mText;
		}

		public boolean isRaw() {
			return mRaw;
		}

	}

	public DialogScrollingText(String displayName, JsonElement element, ActionNested parent) throws Exception {
		super(element.getAsJsonObject());

		mParent = parent;
		mDisplayName = displayName;
		JsonObject jsonObject = element.getAsJsonObject();
		if (jsonObject == null) {
			throw new Exception("scrolling_text value is not an object!");
		}

		JsonArray array = jsonObject.get("text").getAsJsonArray();
		for (JsonElement jsonElement : array) {
			if (jsonElement.isJsonPrimitive()) {
				mText.add(new ScrollingTextEntry(mDisplayName, jsonElement.getAsString()));
			} else if (jsonElement.isJsonObject()) {
				mText.add(new ScrollingTextEntry(mDisplayName, jsonElement.getAsJsonObject()));
			}
		}

		mActions = new QuestActions("", displayName, EntityType.VILLAGER, 0, jsonObject.get("actions"), parent);

		if (jsonObject.has("trigger_actions_on_last_dialog")) {
			mTriggerActionsOnLastDialog = jsonObject.get("trigger_actions_on_last_dialog").getAsBoolean();
		}

		if (jsonObject.has("click_type")) {
			mClickType = jsonObject.get("click_type").getAsInt();
		}

		if (jsonObject.has("raw")) {
			mRaw = jsonObject.get("raw").getAsBoolean();
		}

		if (jsonObject.has("autoScroll")) {
			mAutoScroll = jsonObject.get("autoScroll").getAsBoolean();
		}

		if (jsonObject.has("radius")) {
			mRadius = jsonObject.get("radius").getAsDouble();
		}
	}

	public QuestActions getActions() {
		return mActions;
	}

	@Override
	public void sendDialog(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		ScrollingTextActive active = new ScrollingTextActive(plugin, player, npcEntity, mText, mActions, prereqs,
			new AreaBounds("", new Point(player.getLocation().subtract(mRadius, mRadius, mRadius)),
				new Point(player.getLocation().add(mRadius, mRadius, mRadius))), mRaw, mAutoScroll, mTriggerActionsOnLastDialog);

		String metakey = com.playmonumenta.scriptedquests.Constants.PLAYER_SCROLLING_DIALOG_METAKEY;
		player.setMetadata(metakey, new FixedMetadataValue(plugin, active));

		active.next();

		if (mAutoScroll) {
			active.toggleScroll();
		}
	}

	@Override
	public ActionNested getParent() {
		return mParent;
	}

	@Override
	public QuestPrerequisites getPrerequisites() {
		return null;
	}

	@Override
	public List<ActionQuest> getQuestActions() {
		return new ArrayList<>();
	}

	@Override
	public List<QuestComponent> getQuestComponents(Entity entity) {
		return Collections.emptyList();
	}
}
