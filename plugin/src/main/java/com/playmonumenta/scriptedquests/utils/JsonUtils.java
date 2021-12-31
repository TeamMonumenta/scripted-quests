package com.playmonumenta.scriptedquests.utils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;

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

	public static String getString(JsonObject object, String property, String defaultValue) throws Exception {
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

}
