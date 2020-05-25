package com.playmonumenta.scriptedquests.plots;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.command.CommandSender;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class PlotStyle {
	protected final String mName;
	private Map<String, PlotStyleVariant> mVariants = new HashMap<String, PlotStyleVariant>();

	protected PlotStyle(CommandSender sender, JsonObject object) throws Exception {
		mVariants.clear();

		if (object == null) {
			throw new Exception("object may not be null.");
		}

		// Load the style name
		if (object.get("name") == null ||
		    object.get("name").getAsString() == null ||
		    object.get("name").getAsString().isEmpty()) {
			throw new Exception("Failed to parse 'name'");
		}
		mName = object.get("name").getAsString();

		// Load the property groups - why yes, this section is rather long.
		if (object.get("variants") == null ||
		    object.get("variants").getAsJsonObject() == null) {
			throw new Exception("Failed to parse 'variants'");
		}
		JsonObject variantsJson = object.get("variants").getAsJsonObject();
		for (Map.Entry<String, JsonElement> ent : variantsJson.entrySet()) {
			String varName = ent.getKey();
			if (varName.isEmpty()) {
				throw new Exception("Variant name may not be empty");
			}
			JsonElement variantJson = ent.getValue();
			if (variantJson == null || variantJson.getAsJsonObject() == null) {
				throw new Exception("Failed to parse variant '" + varName + "'; expected an object");
			}
			PlotStyleVariant variant = new PlotStyleVariant(sender, this, varName, variantJson.getAsJsonObject());
			mVariants.put(varName, variant);
		}
	}

	public JsonObject toJson() {
		JsonObject variantsJson = new JsonObject();
		for (Map.Entry<String, PlotStyleVariant> ent : mVariants.entrySet()) {
			String varName = ent.getKey();
			PlotStyleVariant variant = ent.getValue();

			variantsJson.add(varName, variant.toJson());
		}

		JsonObject result = new JsonObject();
		result.addProperty("name", mName);
		result.add("variants", variantsJson);
		return result;
	}

	protected int getNumVariants() {
		return mVariants.size();
	}

	public String getName() {
		return mName;
	}

	public PlotStyleVariant getVariant(String variantName) {
		return mVariants.get(variantName);
	}
}
