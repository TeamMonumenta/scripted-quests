package pe.scriptedquests;

import java.util.Arrays;
import java.util.List;

public class Constants {
	// Metadata keys
	public static final String PLAYER_CLICKABLE_DIALOG_METAKEY = "ScriptedQuestsPlayerClickableDialog";
	public static final String PLAYER_QUEST_ACTIONS_LOCKED_METAKEY = "ScriptedQuestsPlayerQuestActionsLocked";
	public static final String PLAYER_RESPAWN_POINT_METAKEY = "ScriptedQuestsPlayerRespawnPoint";

	public static final List<String> ALL_METAKEYS = Arrays.asList(
		PLAYER_CLICKABLE_DIALOG_METAKEY,
		PLAYER_QUEST_ACTIONS_LOCKED_METAKEY,
		PLAYER_RESPAWN_POINT_METAKEY
	);
}
