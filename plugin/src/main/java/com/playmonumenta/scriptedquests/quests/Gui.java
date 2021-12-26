package com.playmonumenta.scriptedquests.quests;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.api.JsonObjectBuilder;
import com.playmonumenta.scriptedquests.quests.components.GuiPage;
import com.playmonumenta.scriptedquests.utils.JsonUtils;

public class Gui {

	private final File mFile;
	private final String mLabel;
	private final Map<String, GuiPage> mPages = new HashMap<>();

	public Gui(JsonObject object, File file) throws Exception {
		mFile = file;
		mLabel = JsonUtils.getString(object, "label");
		for (Map.Entry<String, JsonElement> page : JsonUtils.getJsonObject(object, "pages").entrySet()) {
			mPages.put(page.getKey(), new GuiPage(page.getValue().getAsJsonObject()));
		}
	}

	public JsonObject toJson() {
		return new JsonObjectBuilder()
			.add("label", mLabel)
			.add("pages", JsonUtils.toJsonObject(mPages, GuiPage::toJson))
			.build();
	}

	public String getLabel() {
		return mLabel;
	}

	public @Nullable GuiPage getPage(String page) {
		return mPages.get(page);
	}

	public void setPage(String pageName, GuiPage page) {
		mPages.put(pageName, page);
	}

	public String[] getPages() {
		return mPages.keySet().toArray(new String[0]);
	}

	public File getFile() {
		return mFile;
	}

}
