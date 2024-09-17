package com.playmonumenta.scriptedquests;

import java.util.Map;
import org.bukkit.SoundCategory;

public class Constants {
	// Metadata keys
	// NOTE: If new metadata keys are added, must remove them in PlayerListener PlayerQuitEvent to prevent memory leaks
	public static final String PLAYER_CLICKABLE_DIALOG_METAKEY = "ScriptedQuestsPlayerClickableDialog";
	public static final String PLAYER_RESPAWN_POINT_METAKEY = "ScriptedQuestsPlayerRespawnPoint";
	public static final String PLAYER_RESPAWN_ACTIONS_METAKEY = "ScriptedQuestsPlayerRespawnActions";
	public static final String PLAYER_DEATH_LOCATION_METAKEY = "ScriptedQuestsPlayerDeathLocation";
	public static final String PLAYER_VOICE_OVER_METAKEY = "ScriptedQuestsPlayerVoiceOver";
	public static final String PLAYER_BOOK_EDITING_METAKEY= "ScriptedQuestsPlayerBookEditing";

	public static final String API_CHANNEL_ID = "scripted-quests:api";

	public static final Map<String, SoundCategory> SOUND_CATEGORY_BY_NAME = Map.of(
		"master", SoundCategory.MASTER,
		"music", SoundCategory.MUSIC,
		"record", SoundCategory.RECORDS,
		"weather", SoundCategory.WEATHER,
		"block", SoundCategory.BLOCKS,
		"hostile", SoundCategory.HOSTILE,
		"neutral", SoundCategory.NEUTRAL,
		"players", SoundCategory.PLAYERS,
		"ambient", SoundCategory.AMBIENT,
		"voice", SoundCategory.VOICE
	);

	public static final Map<SoundCategory, String> SOUND_CATEGORY_NAMES = Map.of(
		SoundCategory.MASTER, "master",
		SoundCategory.MUSIC, "music",
		SoundCategory.RECORDS, "record",
		SoundCategory.WEATHER, "weather",
		SoundCategory.BLOCKS, "block",
		SoundCategory.HOSTILE, "hostile",
		SoundCategory.NEUTRAL, "neutral",
		SoundCategory.PLAYERS, "players",
		SoundCategory.AMBIENT, "ambient",
		SoundCategory.VOICE, "voice"
	);
}
