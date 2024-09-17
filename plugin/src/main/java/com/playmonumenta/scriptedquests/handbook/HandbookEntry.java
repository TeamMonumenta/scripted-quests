package com.playmonumenta.scriptedquests.handbook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

/**
 * A handbook entry is a page of the handbook.
 * <p>
 * An entry should probably not have
 * so much text that it scrolls into a new page; that is way too much text and a player is unlikely to
 * be able to focus long enough to read it. Information should be kept as short as possible. However,
 * we allow multiple pages if you want them
 */
public final class HandbookEntry {
	private final String name;
	private final String permission;
	private final Set<Component> pages;
	private Category category;
	private final String unlockDescription;

	// the keys of the json entries
	private static final String JSON_KEY_NAME = "name";
	private static final String JSON_KEY_UNLOCK_DESCRIPTION  = "unlockDescription";
	private static final String JSON_KEY_PAGES  = "pages";

	/**
	 * @param name       The name of this entry.
	 * @param unlockDescription Sent to the player upon unlocking the entry. Should be a short summary/lore text
	 * @param pages      The pages of the entry and their contents
	 * @param category   The {@link Category} that this HandbookEntry belongs to.
	 */
	public HandbookEntry(String name,
						 String unlockDescription,
						 Set<Component> pages,
						 Category category) {
		this.name = name;
		this.unlockDescription = unlockDescription;
		this.pages = pages;
		this.category = category;
		this.permission = toPath().toString();
	}

	/**
	 * Returns a Json representation of this entry.
	 * This method serializes pages from components into json.
	 *
	 * @return The json representation of this HandbookEntry.
	 * @see HandbookEntry#JSON_KEY_NAME
	 * @see HandbookEntry#JSON_KEY_PAGES
	 * @see HandbookEntry#JSON_KEY_UNLOCK_DESCRIPTION
	 */
	public JsonObject getAsJsonObject() {
		final var obj = new JsonObject();
		obj.addProperty(JSON_KEY_NAME, name);
		obj.addProperty(JSON_KEY_UNLOCK_DESCRIPTION, unlockDescription);
		final var pages = new JsonArray();
		this.pages.forEach(page -> pages.add(GsonComponentSerializer.gson().serializeToTree(page)));
		obj.add(JSON_KEY_PAGES, pages);
		return obj;
	}


	public Path toPath() {
		return category.getEntryFolder().resolve(name + ".json");
	}

	/** Creates a HandbookEntry from a JsonObject and assigns its parent to the given category.
	 *
	 * @param object The object to read
	 * @param category The parent category of this entry.
	 * @return A {@link HandbookEntry} with the given {@link Category} as its parent
	 */
	public static HandbookEntry fromJsonObject(JsonObject object, Category category) {
		final var name = object.get(JSON_KEY_NAME).getAsString();
		final var unlockDescription = object.get(JSON_KEY_UNLOCK_DESCRIPTION).getAsString();
		final var pages = new HashSet<Component>();
		object.getAsJsonArray(JSON_KEY_PAGES).forEach(page -> pages.add(GsonComponentSerializer.gson().deserializeFromTree(page)));
		return new HandbookEntry(name, unlockDescription, pages, category);
	}

	public String name() {
		return name;
	}

	/** Always equivalent to {@code toPath().toString()}
	 *
	 * @return The permission for this entry.
	 */
	public String permission() {
		return permission;
	}

	public Set<Component> pages() {
		return pages;
	}

	public Category category() {
		return category;
	}

	public String unlockDescription() {
		return unlockDescription;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (HandbookEntry) obj;
		return Objects.equals(this.name, that.name) &&
			Objects.equals(this.permission, that.permission) &&
			Objects.equals(this.pages, that.pages) &&
			Objects.equals(this.category, that.category);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, permission, pages, category);
	}

	/** Re-assign to another category.
	 *
	 * @param category The category
	 */
	public void setCategory(Category category) {
		this.category = category;
	}

	@Override
	public String toString() {
		return "HandbookEntry[" +
			"name=" + name + ", " +
			"permission=" + permission + ", " +
			"pages=" + pages + ", " +
			"category=" + category + ']';
	}


}
