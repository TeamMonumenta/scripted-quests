package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.point.AreaBounds;
import com.playmonumenta.scriptedquests.point.Point;
import com.playmonumenta.scriptedquests.quests.components.QuestActions;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionQuestMarker;
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

public class DialogScrollingText extends ActionQuestMarker implements DialogBase {
	private Double mRadius = 4.0;
	private String mDisplayName;
	private ArrayList<String> mText = new ArrayList<String>();
	private QuestActions mActions;
	public int mClickType = 0;
	private boolean mRaw = false;
	private boolean mAutoScroll = false;

	public DialogScrollingText(String displayName, JsonElement element) throws Exception {
		super(element.getAsJsonObject());

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

	@Override
	public void sendDialog(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		ScrollingTextActive active = new ScrollingTextActive(plugin, player, npcEntity, mText, mActions, prereqs,
			new AreaBounds("", new Point(player.getLocation().subtract(mRadius, mRadius, mRadius)),
				new Point(player.getLocation().add(mRadius, mRadius, mRadius))), mRaw, mAutoScroll);

		String metakey = com.playmonumenta.scriptedquests.Constants.PLAYER_SCROLLING_DIALOG_METAKEY;
		player.setMetadata(metakey, new FixedMetadataValue(plugin, active));

		active.next();

		if (mAutoScroll) {
			active.toggleScroll();
		}
	}
}
