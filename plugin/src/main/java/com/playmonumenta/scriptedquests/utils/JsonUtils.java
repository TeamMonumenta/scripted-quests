package com.playmonumenta.scriptedquests.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;

import java.util.*;
import java.util.function.Function;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public abstract class JsonUtils {

	private JsonUtils() {
	}

	public static JsonElement getElement(JsonObject object, String property) throws Exception {
		JsonElement element = object.get(property);
		if (element == null) {
			throw new Exception("'" + property + "' entry is required");
		}
		return element;
	}

	public static JsonObject getJsonObject(JsonObject object, String property) throws Exception {
		JsonElement element = getElement(object, property);
		if (!element.isJsonObject()) {
			throw new Exception("'" + property + "' entry must be an object");
		}
		return element.getAsJsonObject();
	}

	public static JsonArray getJsonArray(JsonObject object, String property) throws Exception {
		JsonElement element = getElement(object, property);
		if (!element.isJsonArray()) {
			throw new Exception("'" + property + "' entry must be an array");
		}
		return element.getAsJsonArray();
	}

	public static String getString(JsonObject object, String property) throws Exception {
		JsonElement element = getElement(object, property);
		if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
			throw new Exception("'" + property + "' entry must be a string");
		}
		return element.getAsString();
	}

	public static @Nullable String getString(JsonObject object, String property, @Nullable String defaultValue) throws Exception {
		JsonElement element = object.get(property);
		if (element == null) {
			return defaultValue;
		}
		if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
			throw new Exception("'" + property + "' entry must be a string");
		}
		return element.getAsString();
	}

	public static int getInt(JsonObject object, String property) throws Exception {
		JsonElement element = getElement(object, property);
		if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
			throw new Exception("'" + property + "' entry must be a number");
		}
		return element.getAsInt();
	}

	public static int getInt(JsonObject object, String property, int defaultValue) throws Exception {
		JsonElement element = object.get(property);
		if (element == null) {
			return defaultValue;
		}
		if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
			throw new Exception("'" + property + "' entry must be a number");
		}
		return element.getAsInt();
	}

	public static double getDouble(JsonObject object, String property) throws Exception {
		JsonElement element = getElement(object, property);
		if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
			throw new Exception("'" + property + "' entry must be a number");
		}
		return element.getAsDouble();
	}

	public static double getDouble(JsonObject object, String property, double defaultValue) throws Exception {
		JsonElement element = object.get(property);
		if (element == null) {
			return defaultValue;
		}
		if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
			throw new Exception("'" + property + "' entry must be a number");
		}
		return element.getAsDouble();
	}

	public static boolean getBoolean(JsonObject object, String property) throws Exception {
		JsonElement element = getElement(object, property);
		if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
			throw new Exception("'" + property + "' entry must be a boolean");
		}
		return element.getAsBoolean();
	}

	public static boolean getBoolean(JsonObject object, String property, boolean defaultValue) throws Exception {
		JsonElement element = object.get(property);
		if (element == null) {
			return defaultValue;
		}
		if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
			throw new Exception("'" + property + "' entry must be a boolean");
		}
		return element.getAsBoolean();
	}

	public static <T> JsonObject toJsonObject(Map<String, T> map, Function<T, JsonElement> toJsonFunc) {
		JsonObjectBuilder builder = new JsonObjectBuilder();
		for (Map.Entry<String, T> entry : map.entrySet()) {
			builder.add(entry.getKey(), toJsonFunc.apply(entry.getValue()));
		}
		return builder.build();
	}

	public static <T> JsonArray toJsonArray(List<T> list, Function<T, JsonElement> toJsonFunc) {
		JsonArray array = new JsonArray();
		for (T element : list) {
			array.add(toJsonFunc.apply(element));
		}
		return array;
	}


	/** A mapping function over a JsonArray to turn it into a java List
	 *
	 * @param array The JsonArray in question
	 * @param how  The way to convert into <code>T</code>
	 * @return A List of a generic type created by applying a function over a given json array.
	 * @param <T> The type of the list
	 */
	public static <T> List<T> intoList(JsonArray array, Function<JsonElement, ? extends T> how) {
		List<T> accumulator = new ArrayList<>();
		for (var item : array) {
			accumulator.add(how.apply(item));
		}
		return accumulator;
	}

	/** A mapping function over a JsonArray to turn it into a java Set
	 *
	 * @param array The JsonArray in question
	 * @param how  The way to convert into <code>T</code>
	 * @return A Set of type <code>T</code> created by applying a function over a given json array.
	 * @param <T> The type of the Set
	 */
	public static <T> Set<T> intoSet(JsonArray array, Function<JsonElement, ? extends T> how) {
		Set<T> accumulator = new HashSet<>();
		for (var item : array) {
			accumulator.add(how.apply(item));
		}
		return accumulator;
	}

	public static <T> T parse(JsonObject object, String property, Function<String, T> parser) throws Exception {
		return parser.apply(getString(object, property));
	}

	public static <T> T parse(JsonObject object, String property, Function<String, T> parser, T defaultValue) throws Exception {
		JsonElement element = object.get(property);
		if (element == null) {
			return defaultValue;
		}
		if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
			throw new Exception("'" + property + "' entry must be a string");
		}
		return parser.apply(element.getAsString());
	}

	public static @Nullable Material getMaterial(JsonObject object, String property, @Nullable Material defaultValue) throws Exception {
		return parse(object, property, val -> {
			try {
				return Material.valueOf(val);
			} catch (IllegalArgumentException e) {
				throw new RuntimeException("Unknown Material '" + val +
					                           "' - it should be one of the values in this list: " +
					                           "https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html");
			}
		}, defaultValue);
	}
}
