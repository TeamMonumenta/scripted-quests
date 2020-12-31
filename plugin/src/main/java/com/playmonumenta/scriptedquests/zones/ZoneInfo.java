package com.playmonumenta.scriptedquests.zones;

import com.playmonumenta.scriptedquests.quests.components.QuestComponent;

import java.util.ArrayList;
import java.util.List;

public class ZoneInfo {

	public final String mId;
	public final String mName;
	public int mXP = 0;

	public boolean mDiscoverySounds = true;
	public List<QuestComponent> mComponents = new ArrayList<>();
	public ZoneInfo(String id, String name) {
		mId = id;
		mName = name;
	}

}
