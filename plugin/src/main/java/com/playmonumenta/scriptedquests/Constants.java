package com.playmonumenta.scriptedquests;

public class Constants {
	// Metadata keys
	// NOTE: If new metadata keys are added, must remove them in PlayerListener PlayerQuitEvent to prevent memory leaks
	public static final String PLAYER_CLICKABLE_DIALOG_METAKEY = "ScriptedQuestsPlayerClickableDialog";
	public static final String PLAYER_RESPAWN_POINT_METAKEY = "ScriptedQuestsPlayerRespawnPoint";
	public static final String PLAYER_RESPAWN_ACTIONS_METAKEY = "ScriptedQuestsPlayerRespawnActions";
	public static final String PLAYER_DEATH_LOCATION_METAKEY = "ScriptedQuestsPlayerDeathLocation";
	public static final String PLAYER_VOICE_OVER_METAKEY = "ScriptedQuestsPlayerVoiceOver";

	public static final String API_CHANNEL_ID = "scripted-quests:api";
}
