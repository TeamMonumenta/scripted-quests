package com.playmonumenta.scriptedquests.handbook;

import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;


/**
 * A Category holds {@link HandbookEntry HandbookEntries},
 * one can imagine categories like directories, but instead of files, they hold handbook entries.
 * A category is the root of a certain type of handbook entries, and has a title page to explain what it is about.
 * i.e. the Region 3 category is a directory to all the region three handbook entries.
 */
public final class Category {
	private final String name;
	private final String titlePage;
	private Category parent;
	private final Set<HandbookEntry> entries;

	/**
	 * @param name      The name of this category
	 * @param titlePage The title page of this category. Should be used to explain what it is.
	 * @param parent    The parent category that this category belongs to. Only one Category should have a null parent: the title page for the handbook.
	 * @param entries   The handbook entries that this Category contains. Should not be empty.
	 */
	public Category(String name,
					String titlePage,
					Category parent,
					Set<HandbookEntry> entries) {
		this.name = name;
		this.titlePage = titlePage;
		this.parent = parent;
		this.entries = entries;
	}


	/**
	 * @return A list of all of this category's parents, from bottom to top
	 */
	public List<Category> traceParents() {
		return Stream.iterate(parent, Objects::nonNull, Category::parent).toList();
	}

	/**
	 * get the directory of the category
	 *
	 * @return
	 */
	public Path toDirectoryPath() {
		final var cs = traceParents();
		final var buff = new StringBuilder();
		for (var i = cs.size() - 1; i >= 0; i--) {
			buff.append(cs.get(i).name).append("/");
		}
		buff.append(name);
		return Path.of(buff.toString());
	}


	/**
	 * get the directory of the ENTRIES of this category.
	 *
	 * @return todo documentation
	 */
	public Path getEntriesPath() {
		final var cs = traceParents();
		final var buff = new StringBuilder();
		for (var i = cs.size() - 1; i >= 0; i--) {
			buff.append(cs.get(i).name).append("/");
		}
		buff.append(name).append("entries");
		return Path.of(buff.toString());
	}

	/**
	 * Creates a path representation to the category json file.
	 *
	 * @return A path representation to the category json file.
	 */
	public Path jsonFilePath() {
		return Path.of(toDirectoryPath() + "/category_info.json");
	}

	/**
	 * A non-titled page which is just links to all the entries in this category.
	 *
	 * @return A page which is just links to all the handbook entries inside the category.
	 */
	public static String defaultPage() {
		return "";
	}

	/**
	 * Creates a <code>JsonObject</code> representation of this <code>Category</code>.
	 * The object takes structure as:
	 * <pre>
	 * {@code
	 * {
	 *     name: String,
	 *     titlePage: String,
	 *     parentPath: Optional[String]
	 *     entries: [HandbookEntry]
	 * }
	 *
	 * }
	 * </pre>
	 *
	 * @return This <code>Category</code> as a <code>JsonObject</code>
	 */
	public JsonObject getAsJsonObject() {
		final var obj = new JsonObject();
		obj.addProperty("name", name);
		obj.addProperty("titlePage", titlePage);
		if (parent != null) {
			obj.addProperty("parentPath", parent.toDirectoryPath().toString());
		}
//		final var entries = new JsonArray();
//		this.entries.forEach(entry -> entries.add(entry.getAsJsonObject()));
//		obj.add("entries", entries);
		return obj;
	}


	/**
	 * The permission of a category is simply the path of the category with / replaced with ".".
	 *
	 * @return The permission.
	 */
	public String permission() {
		return toDirectoryPath().toString().replaceAll("/", ".");
	}

	public void assignParent(Category parent) {
		this.parent = parent;
	}

	public String name() {
		return name;
	}

	public String titlePage() {
		return titlePage;
	}

	public Category parent() {
		return parent;
	}

	public Set<HandbookEntry> entries() {
		return entries;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (Category) obj;
		return Objects.equals(this.name, that.name) &&
			Objects.equals(this.titlePage, that.titlePage) &&
			Objects.equals(this.parent, that.parent) &&
			Objects.equals(this.entries, that.entries);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, titlePage, parent, entries);
	}

	@Override
	public String toString() {
		return "Category[" +
			"name=" + name + ", " +
			"titlePage=" + titlePage + ", " +
			"parent=" + parent + ", " +
			"entries=" + entries + ']';
	}

	public void setParent(Category parent) {
		this.parent = parent;
	}

}
