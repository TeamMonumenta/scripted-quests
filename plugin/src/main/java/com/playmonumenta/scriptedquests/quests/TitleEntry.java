package com.playmonumenta.scriptedquests.quests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.components.AnimatedTitle;
import com.playmonumenta.scriptedquests.quests.components.ScrollingTitle;
import com.playmonumenta.scriptedquests.quests.components.StaticTitle;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.title.Title.Times;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.entity.Player;

/** A TitleEntry
 * @author Tristian
 *
 * A title entry is a sort of animated title, with "instructions" to transform the title.
 * Executed by using /sqtitle <player> <label>
 */
public abstract class TitleEntry {

	private final String label;
	private final int stay;
	private final int fadeIn;
	private final int fadeOut;

	public TitleEntry(JsonObject object) {

		label = Objects.requireNonNull(object.get("label").getAsString(), "A `label` is required.");
		stay = object.get("stay").getAsInt(); // (in ticks)
//		optional parameters
		fadeIn = Optional.ofNullable(object.get("fadeIn")).map(JsonElement::getAsInt).orElse(0);
		fadeOut = Optional.ofNullable(object.get("fadeOut")).map(JsonElement::getAsInt).orElse(0);


	}

	/** Reads off a TitleEntry from a JsonObject
	 * @see Type#read(JsonObject)
	 * @see TitleEntry#TitleEntry(JsonObject)
	 *
	 * @param object The object to read off of.
	 * @return The read title entry.
	 */
	public static TitleEntry read(JsonObject object) {
		return Objects.requireNonNull(Type.fromString(object.get("type").getAsString()).orElseThrow(() -> new IllegalArgumentException("A `type` is required."))).create(object);
	}


	public String getLabel() {
		return label;
	}


	/**
	 * A title entry can hold one of these types.
	 * They are defined as "static" (a vanilla title),
	 * "scrolling" (text that scrolls from left to right with a set delay for each character),
	 * and "animated", where the user defines their own set of "instructions" for the title.
	 */
	enum Type {
		STATIC("static", StaticTitle.class),
		SCROLLING("scrolling", ScrollingTitle.class),
		ANIMATED("animated", AnimatedTitle.class);

		private final String jsonTag;
		private final Class<?> clazz;

		Type(String jsonTag, Class<? extends TitleEntry> clazz) {
			this.jsonTag = jsonTag;
			this.clazz = clazz;
		}
		TitleEntry create(JsonObject object) {
			try {
				return (TitleEntry) clazz.getDeclaredConstructors()[0].newInstance(object);
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}

		/** Parse out a type from a string given by the json.
		 *
		 * @param what The string.
		 * @return A type from the json, or null.
		 */
		static Optional<Type> fromString(String what) {
			for (Type t: values()) {
				if (what.equals(t.jsonTag)) return Optional.of(t);
			}
			return Optional.empty();
		}
	}

	/** Run the title on the player.
	 *
	 * @param player The player to run for.
	 */
	public abstract void runTitle(Player player);

	/** Sets the TIMES component of the title for the player.
	 *
	 * @param player The player to send to.
	 */
	protected void sendTimes(Player player) {
		player.sendTitlePart(TitlePart.TIMES, Times.times(Duration.ofMillis(fadeIn), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut)));
	}


}
