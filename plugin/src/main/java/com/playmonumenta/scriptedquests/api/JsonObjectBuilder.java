package com.playmonumenta.scriptedquests.api;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class JsonObjectBuilder {
	JsonObject object;

	private JsonObjectBuilder() {
		object = new JsonObject();
	}

	public static JsonObjectBuilder get() {
		return new JsonObjectBuilder();
	}

	public JsonObjectBuilder add(String key, JsonElement e) {
		object.add(key, e);
		return this;
	}

	public JsonObjectBuilder add(String key, String e) {
		return add(key, new JsonPrimitive(e));
	}

	public JsonObjectBuilder add(String key, int e) {
		return add(key, new JsonPrimitive(e));
	}

	public JsonObjectBuilder add(String key, double e) {
		return add(key, new JsonPrimitive(e));
	}

	public JsonObjectBuilder add(String key, boolean e) {
		return add(key, new JsonPrimitive(e));
	}

	public JsonObjectBuilder add(String key, List<JsonElement> e) {
		JsonArray a = new JsonArray();
		for (JsonElement elem : e) {
			a.add(elem);
		}

		object.add(key, a);
		return this;
	}

	public JsonObject build() {
		return object;
	}
}
