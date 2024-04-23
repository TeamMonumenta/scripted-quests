package com.playmonumenta.scriptedquests.quests.components;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.TitleEntry;
import java.util.Objects;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** A title with a fully custom set of animation instructions.
 * Each instruction must be of the form { target: "subtitle" | "title", (wait | app | sub) : (appropriate value) }
 * @author Tristian
 */
public class AnimatedTitle extends TitleEntry {



	private final JsonArray instructionSet;


	public AnimatedTitle(JsonObject object) {
		super(object);
		this.instructionSet = object.getAsJsonArray("instructions");
	}

	@Override
	public void runTitle(Player player) {
		sendTimes(player);
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			TextComponent currentTitle = Component.empty();
			player.sendTitlePart(TitlePart.TITLE, currentTitle); // player must have a title for subtitles to show.
			TextComponent currentSubtitle = Component.empty();
			for (var instruction: instructionSet) {
				if (!instruction.isJsonObject()) throw new IllegalArgumentException(instruction + " is not a valid AnimatedTitle instruction.");
				var obj = instruction.getAsJsonObject();
				var f = Objects.requireNonNull(Instruction.fromObject(obj), instruction + " does not supply an instruction or supplies one that is invalid.");
				var target = "title".equals(obj.get("target").getAsString()) ? TitlePart.TITLE : TitlePart.SUBTITLE;
				if (target == TitlePart.TITLE) {
					currentTitle = f.apply(currentTitle);
					player.sendTitlePart(target, currentTitle);
				}
				if (target == TitlePart.SUBTITLE) {
					currentSubtitle = f.apply(currentSubtitle);
					player.sendTitlePart(target, currentSubtitle);
				}
			}
		});
	}


	/**
	 * The set of "Instructions" for an animation.
	 * These can be:
	 * <ul>
	 *     <li><code>wait</code>, pausing for the specified ms</li>
	 *     <li><code>sub(tract)</code>, subtract characters off the end</li>
	 *     <li><code>app(end)</code>, append characters to the end</li>
	 * </ul>
	 */
	enum Instruction {
		WAIT("wait", number -> component -> {
			try {
				Thread.sleep(number.getAsInt());
			} catch (InterruptedException e) {
				Plugin.getInstance().getLogger().severe("AnimatedTitle WAIT instruction was interrupted.");
			}
			return component;
		}),
		APPEND("app", c -> component -> component.content(component.content()+c.getAsString())), // append a string
		SUBTRACT("sub", i -> component -> component.content(component.content().substring(component.content().length() - i.getAsInt()))); // subtract from the end.

		private final String jsonKey;
		private final Function<JsonPrimitive, Function<TextComponent, TextComponent>>  f;
		Instruction(String jsonKey, Function<JsonPrimitive, Function<TextComponent, TextComponent>> f) {
			this.jsonKey = jsonKey;
			this.f = f;
		}

		/**
		 *
		 * @param object The object to pull from.
		 * @return A bound function for use to transform the state of the AnimatedTitle.
		 */
		static Function<TextComponent, TextComponent> fromObject(JsonObject object) {
			for (Instruction i: values()) {
				if (object.has(i.jsonKey)) return i.f.apply(object.get(i.jsonKey).getAsJsonPrimitive());
			}
			return null;
		}

	}


}