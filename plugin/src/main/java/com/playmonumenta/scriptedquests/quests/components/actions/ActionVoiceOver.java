package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class ActionVoiceOver implements ActionBase {
	private final int mMaxVOPerPlayer = 12;
	private final String mNpcKey;
	private final String mSound;

	public ActionVoiceOver(EntityType entityType, String npcName, JsonElement element) throws Exception {
		mNpcKey = entityType.toString() + ", " + npcName;
		mSound = element.getAsString();
		if (mSound == null) {
			throw new Exception("Voice Over value is not a string!");
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void doAction(QuestContext context) {
		Player player = context.getPlayer();
		Map<String, String> playerLastSounds;
		if (player.hasMetadata(Constants.PLAYER_VOICE_OVER_METAKEY)) {
			playerLastSounds = (Map<String, String>) player.getMetadata(Constants.PLAYER_VOICE_OVER_METAKEY).get(0).value();
		} else {
			// LRU map via https://stackoverflow.com/a/11469731
			playerLastSounds = new LinkedHashMap<String, String>(mMaxVOPerPlayer + 1, 1.0f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
					if (size() > mMaxVOPerPlayer) {
						player.stopSound(eldest.getValue(), SoundCategory.VOICE);
						return true;
					}
					return false;
				}
			};
		}

		String npcKey = mNpcKey;
		if (context.getNpcEntity() != null) {
			npcKey = context.getNpcEntity().getUniqueId().toString();
		}

		String lastSound = playerLastSounds.get(npcKey);
		if (lastSound != null) {
			player.stopSound(lastSound, SoundCategory.VOICE);
			playerLastSounds.remove(npcKey);
		}

		Entity entity = player;
		if (context.getNpcEntity() != null) {
			entity = context.getNpcEntity();
		}
		Location location = entity.getLocation().add(0, entity.getHeight(), 0);
		float volume = 1.0f;
		float pitch = 1.0f;
		player.playSound(location, mSound, SoundCategory.VOICE, volume, pitch);
		playerLastSounds.put(npcKey, mSound);
		player.setMetadata(Constants.PLAYER_VOICE_OVER_METAKEY, new FixedMetadataValue(context.getPlugin(), playerLastSounds));
	}
}
