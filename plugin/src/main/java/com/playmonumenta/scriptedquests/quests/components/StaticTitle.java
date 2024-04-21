package com.playmonumenta.scriptedquests.quests.components;

import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.TitleEntry;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.entity.Player;

/**
 * @author Tristian
 * This is a static title, basically just a vanilla title.
 * Either one or both of title and subtitle are required.
 * A sample json object:
 * <pre>
 * {
 * 	   "label": "example",
 *     "stay": 200,
 *     "title": "<red>some title</red>",
 *     "subtitle": "<blue>some subtitle</blue>"
 * }
 * </pre>
 */
public class StaticTitle extends TitleEntry {

	private final Component title;
	private final Component subtitle;

	public StaticTitle(JsonObject object) {
		super(object);
		MiniMessage mm = MiniMessage.miniMessage();
		this.title = Optional.ofNullable(object.get("title").getAsString()).map(mm::deserialize).orElse(Component.empty());
		this.subtitle = Optional.ofNullable(object.get("subtitle").getAsString()).map(mm::deserialize).orElse(Component.empty());
		if (title == Component.empty() && subtitle == Component.empty()) throw new IllegalArgumentException("Either one or both of fields `title` and `subtitle` are required.");

	}

	@Override
	public void runTitle(Player player) {
		sendTimes(player);
		player.sendTitlePart(TitlePart.TITLE, title);
		player.sendTitlePart(TitlePart.SUBTITLE, subtitle);
	}
}
