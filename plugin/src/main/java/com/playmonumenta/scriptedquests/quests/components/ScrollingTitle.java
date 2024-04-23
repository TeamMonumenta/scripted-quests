package com.playmonumenta.scriptedquests.quests.components;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.TitleEntry;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** A scrolling (sub)title with a constant delay (in ms) for each character. Scrolls from left to right.
 * The user can specify either a subtitle, or a title, or both; but not none.
 * These will be JsonObjects containing the delay constant, and the component (text) itself.
 * A `parallel` field is only required if both subtitle and title are provided.
 * If <code>parallel</code> is <code>true</code>, and both subtitle and title is provided, scrolling will run in parallel: both the title and subtitle will scroll at the same time.
 * If <code>parallel</code> is <code>false</code> and both subtitle and title is provided, a <code>wait</code> time can be provided (in ms).
 * the subtitle will wait for <code>wait</code> ms after the title is done before starting, or run instantly after the title is done if not provided.
 *
 * <p>
 *
 * <b>Example:</b>
 * <pre>
 *  {
 *	"label" : "scrolling_example",
 *	"stay" : 120,
 *	"type" : "scrolling",
 *	"parallel : true,
 * 	"title": {
 * 	    "delay": 50,
 * 	    "text":"<red>this is my scrolling title</red>"
 * 	},
 * 	"subtitle": {
 * 	    "delay": 50,
 * 	    "text":"<blue>with a scrolling subtitle.</blue>"
 * 	}
 *  }
 *
 * </pre>
 *
 * @author Tristian
 *
 */
public class ScrollingTitle extends TitleEntry  {

	final JsonObject title, subtitle;
	final boolean parallel;

	public ScrollingTitle(JsonObject object) {
		super(object);
		this.title = object.getAsJsonObject("title");
		this.subtitle = object.getAsJsonObject("subtitle");
		this.parallel = this.title != null && this.subtitle != null && Objects.requireNonNull(object.get("parallel"), "A `parallel` field must be provided.").getAsBoolean();

		if (title == null && subtitle == null) throw new IllegalArgumentException("Either one or both of objects `title` and `subtitle` are required.");
	}

	@Override
	public void runTitle(Player player) {
		sendTimes(player);
		player.sendTitlePart(TitlePart.TITLE, Component.empty()); // subtitles don't display without a title.
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			int titleTime = Optional.ofNullable(title)
					.map(x -> runTask(TitlePart.TITLE, x.get("delay").getAsInt(), player, x)
							+ Optional.ofNullable(x.get("wait")).map(JsonElement::getAsInt).orElse(0)) // if a wait time was provided, we add it. This handles the non-parallel case by just defaulting to 0 if it wasn't provided.
					.orElse(0);
			if (!parallel) {
				try {
					Thread.sleep(titleTime);
				} catch (InterruptedException e) {
					Plugin.getInstance().getLogger().severe("The non-parallel ScrollingTitle " + getLabel() + " was interrupted. Aborting it.");
					return;
				}
			}
			Optional.ofNullable(subtitle).ifPresent(x -> runTask(TitlePart.SUBTITLE, x.get("delay").getAsInt(), player, x));
		});
	}

	/**
	 *
	 * @param tp The TitlePart
	 * @param delay The delay constant.
	 * @param p The player to send to.
	 * @param object The object part.
	 * @return The total time this task will take.
	 */
	private int runTask(TitlePart<Component> tp, int delay, Player p, JsonObject object) {
		Plugin.getInstance().getLogger().info("Starting scrolling title " + getLabel() + " for player " + p.getName());
		final MiniMessage mm = MiniMessage.miniMessage();
		TextComponent text = (TextComponent) Optional.ofNullable(object.get("text").getAsString()).map(mm::deserialize).orElse(Component.empty());
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			TextComponent current = Component.empty().style(text.style());
			for (char c: text.content().toCharArray()) {
				p.sendTitlePart(tp, current = current.content(current.content() + c));
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					Plugin.getInstance().getLogger().severe("Task for ScrollingTitle " + getLabel() + " was interrupted. Aborting it.");
					return;
				}
			}
		});
		return delay * text.content().length();
	}
}
