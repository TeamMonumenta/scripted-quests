package com.playmonumenta.scriptedquests.quests;

public enum QuestType {

	MAIN("Main Quest"),
	SIDE("World Quest"),
	CLASS("Class Quest"),
	UNLOCK("Unlock Quest"),
	LEGACY("Legacy Quest");

	private final String mName;
	QuestType(String name) {
		mName = name;
	}

	public String getName() {
		return mName;
	}

}
