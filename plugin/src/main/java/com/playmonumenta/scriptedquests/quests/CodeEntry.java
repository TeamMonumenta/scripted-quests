package com.playmonumenta.scriptedquests.quests;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/**
 * A CodeEntry is a container for an interaction the player can trigger at any time
 * by typing /code <words>
 * <p>
 * Labels must be globally unique
 */
public class CodeEntry {
	public static final String[] words = new String[] {
		"air",
		"bed",
		"bow",
		"dye",
		"egg",
		"end",
		"ice",
		"log",
		"map",
		"tnt",
		"vex",
		"beef",
		"boat",
		"bone",
		"book",
		"cake",
		"clay",
		"coal",
		"dirt",
		"door",
		"fern",
		"fire",
		"gold",
		"iron",
		"kelp",
		"lava",
		"leaf",
		"milk",
		"sand",
		"sign",
		"slab",
		"snow",
		"vine",
		"void",
		"wolf",
		"wood",
		"wool",
		"anvil",
		"apple",
		"arrow",
		"bread",
		"brick",
		"chest",
		"clock",
		"coral",
		"fence",
		"flint",
		"ghast",
		"grass",
		"lapis",
		"lever",
		"magma",
		"melon",
		"paper",
		"plank",
		"slime",
		"stick",
		"stone",
		"sugar",
		"sword",
		"torch",
		"water",
		"wheat",
		"witch",
	};

	public static final ArgumentSuggestions SUGGESTIONS_WORDS = ArgumentSuggestions.strings(words);

	private final ArrayList<QuestComponent> mComponents = new ArrayList<>();
	private final String mSeed;

	public CodeEntry(JsonObject object) throws Exception {
		//////////////////////////////////////// seed (Required) ////////////////////////////////////////
		JsonElement seed = object.get("seed");
		if (seed == null) {
			throw new Exception("'seed' entry is required");
		}
		if (seed.getAsString() == null || seed.getAsString().isEmpty()) {
			throw new Exception("Failed to parse 'seed' as string");
		}
		mSeed = seed.getAsString();

		//////////////////////////////////////// quest_components (Required) ////////////////////////////////////////
		JsonElement questComponents = object.get("quest_components");
		if (questComponents == null) {
			throw new Exception("'quest_components' entry is required");
		}
		JsonArray array = questComponents.getAsJsonArray();
		if (array == null) {
			throw new Exception("Failed to parse 'quest_components' as JSON array");
		}

		for (JsonElement entry : array) {
			// TODO: Refactor so that components only require a linkage to the top-level item, not a name/entity type
			mComponents.add(new QuestComponent("", "", EntityType.PLAYER, entry));
		}

		//////////////////////////////////////// Fail if other keys exist ////////////////////////////////////////
		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("seed") && !key.equals("display_name")
				&& !key.equals("quest_components")) {
				throw new Exception("Unknown quest key: " + key);
			}
		}
	}

	public String getSeed() {
		return mSeed;
	}

	public ArrayList<QuestComponent> getComponents() {
		return mComponents;
	}

	public String getCodeForPlayer(Player player) {
		return getCodeForPlayer(player, mSeed);
	}

	public static String getCodeForPlayer(Player player, String seed) {
		int hash = (player.getUniqueId().toString() + seed).hashCode();
		int hashKey1 = hash & 0B111111;
		int hashKey2 = (hash >> 6) & 0B111111;
		int hashKey3 = (hash >> 12) & 0B111111;

		return words[hashKey1] + " " + words[hashKey2] + " " + words[hashKey3];
	}

	public boolean doActionsIfCodeMatches(Plugin plugin, Player player, String code) {
		String goodCode = getCodeForPlayer(player).toLowerCase().replaceAll("\\s", "");
		code = code.toLowerCase().replaceAll("\\s", "");
		if (goodCode.equals(code)) {
			for (QuestComponent component : mComponents) {
				component.doActionsIfPrereqsMet(new QuestContext(plugin, player, null));
			}

			return true;
		}
		return false;
	}
}
