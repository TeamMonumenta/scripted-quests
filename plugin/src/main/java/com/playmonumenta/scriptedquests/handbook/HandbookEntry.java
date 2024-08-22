package com.playmonumenta.scriptedquests.handbook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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
	private final Set<String> pages;
	private Category category;
	private final String unlockDescription;

	/**
	 * @param name       The name of this entry.
	 * @param unlockDescription Sent to the player upon unlocking the entry. Should be a short summary/lore text
	 * @param permission The permission to unlock this handbook entry. This is automatically handled while parsing.
	 * @param pages      The pages of the entry and their contents
	 * @param category   The {@link Category} that this HandbookEntry belongs to.
	 */
	public HandbookEntry(String name,
						 String unlockDescription,
						 String permission,
						 Set<String> pages,
						 Category category) {
		this.name = name;
		this.unlockDescription = unlockDescription;
		this.permission = permission;
		this.pages = pages;
		this.category = category;
	}

	/**
	 * <p>
	 * The returned object is of the form:
	 * <pre>
	 * {@code
	 * {
	 *     name: String,
	 *     permission: String,
	 *     unlockDescription: String,
	 *     pages: [String]
	 * }
	 *
	 * }
	 * </pre>
	 *
	 * @return The json representation of this HandbookEntry.
	 */
	public JsonObject getAsJsonObject() {
		final var obj = new JsonObject();
		obj.addProperty("name", name);
		obj.addProperty("permission", permission);
		obj.addProperty("unlockDescription", unlockDescription);
		final var pages = new JsonArray();
		this.pages.forEach(pages::add);
		obj.add("pages", pages);
		return obj;
	}


	public Path toPath() {
		return Path.of(category.getEntriesPath() + "/" + name + ".json");
	}

	public static HandbookEntry fromJsonObject(JsonObject object, Category category) {
		final var name = object.get("name").getAsString();
		final var permission = object.get("permission").getAsString();
		final var unlockDescription = object.get("unlockDescription").getAsString();
		final var pages = new HashSet<String>();
		object.getAsJsonArray("pages").forEach(page -> pages.add(page.getAsString()));
		return new HandbookEntry(name, unlockDescription, permission, pages, category);
	}

	public String name() {
		return name;
	}

	public String permission() {
		return permission;
	}

	public Set<String> pages() {
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
