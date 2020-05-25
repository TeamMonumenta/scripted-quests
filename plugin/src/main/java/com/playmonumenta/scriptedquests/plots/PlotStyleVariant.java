package com.playmonumenta.scriptedquests.plots;

import java.util.Map;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.util.Vector;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.utils.JsonUtils;

public class PlotStyleVariant {
	protected final PlotStyle mPlotStyle;
	protected final String mVarName;
	protected final Vector mSize;
	protected final Location mInsideLoc;
	protected final Location mOutsideLoc;

	protected PlotStyleVariant(CommandSender sender, PlotStyle plotStyle, String varName, JsonObject object) throws Exception {
		mPlotStyle = plotStyle;

		if (varName == null || varName.isEmpty()) {
			throw new Exception("Variant name may not be empty or null.");
		}
		mVarName = varName;

		if (object == null) {
			throw new Exception("Variant must be an object.");
		}

		// To get around the compiler warning about assigning finalized values more than once
		Vector size = null;
		Location insideLoc = null;
		Location outsideLoc = null;

		for (Map.Entry<String, JsonElement> ent : object.entrySet()) {
			String key = ent.getKey();
			JsonElement value = ent.getValue();

			switch (key) {
			case "size":
				size = JsonUtils.getAsVector(value, "size");
				break;
			case "inside_location":
				insideLoc = JsonUtils.getAsLocation(value, "inside_location");
				break;
			case "outside_location":
				outsideLoc = JsonUtils.getAsLocation(value, "outside_location");
				break;
			default:
				throw new Exception("Unknown PlotStyleVariant key: " + key);
			}
		}

		if (size == null) {
			throw new Exception("Plot variant " + plotStyle.getName() + ":" + varName + ": size required.");
		}
		if (insideLoc == null) {
			throw new Exception("Plot variant " + plotStyle.getName() + ":" + varName + ": inside_location required.");
		}
		if (outsideLoc == null) {
			throw new Exception("Plot variant " + plotStyle.getName() + ":" + varName + ": outside_location required.");
		}

		mSize = size;
		mInsideLoc = insideLoc;
		mOutsideLoc = outsideLoc;
	}

	protected JsonObject toJson() {
		JsonObject sizeJson = JsonUtils.toJsonObject(mSize);
		JsonObject insideJson = JsonUtils.toJsonObject(mInsideLoc);
		JsonObject outsideJson = JsonUtils.toJsonObject(mOutsideLoc);

		JsonObject result = new JsonObject();
		result.add("size", sizeJson);
		result.add("inside_location", insideJson);
		result.add("outside_location", outsideJson);
		return result;
	}

	public PlotStyle getPlotStyle() {
		return mPlotStyle;
	}

	public String getName() {
		return mVarName;
	}
}
