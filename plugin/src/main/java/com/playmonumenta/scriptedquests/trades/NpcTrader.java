package com.playmonumenta.scriptedquests.trades;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestNpc;
import com.playmonumenta.scriptedquests.utils.FileUtils;

/*
 * An NpcTrader object holds prerequisites for each trade slot in the NPC's inventory
 * Only one NpcTrader object exists per NPC name
 */
public class NpcTrader {
	public static final String TRADER_MODIFIED_METAKEY = "ScriptedQuestsTraderModified";
	private final ArrayList<NpcTrade> mTrades = new ArrayList<NpcTrade>();
	private final String mNpcName;

	public NpcTrader(String fileLocation) throws Exception {
		String content = FileUtils.readFile(fileLocation);
		if (content == null || content.isEmpty()) {
			throw new Exception("File '" + fileLocation + "' is empty?");
		}

		Gson gson = new Gson();
		JsonObject object = gson.fromJson(content, JsonObject.class);
		if (object == null) {
			throw new Exception("Failed to parse file '" + fileLocation + "' as JSON object");
		}

		// Read the npc's name first
		JsonElement npc = object.get("npc");
		if (npc == null) {
			throw new Exception("'npc' entry for quest '" + fileLocation + "' is required");
		}
		if (npc.getAsString() == null || QuestNpc.squashNpcName(npc.getAsString()).isEmpty()) {
			throw new Exception("Failed to parse 'npc' name for file '" +
								fileLocation + "' as string");
		}
		mNpcName = QuestNpc.squashNpcName(npc.getAsString());

		// Read the npc's trades
		JsonArray array = object.getAsJsonArray("trades");
		if (array == null) {
			throw new Exception("Failed to parse 'trades' for file '"
								+ fileLocation + "' as JSON array");
		}
		Iterator<JsonElement> iter = array.iterator();
		while (iter.hasNext()) {
			JsonElement entry = iter.next();

			mTrades.add(new NpcTrade(entry));
		}

		// Iterate through the remaining keys and throw an error if any are found
		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("npc") && !key.equals("trades")) {
				throw new Exception("Unknown NpcTrader key: " + key);
			}
		}

		// Sort the list of trades in reverse / descending order
		Collections.sort(mTrades, Collections.reverseOrder());

		// Step through the list - if you ever find the same index twice in a row, throw an error
		int lastIndex = -1;
		for (NpcTrade trade : mTrades) {
			if (trade.getIndex() == lastIndex) {
				throw new Exception("Trader '" + mNpcName + "' specifies index " + Integer.toString(lastIndex) + " more than once");
			}
			lastIndex = trade.getIndex();
		}
	}

	/*
	 * NOTE: This is always the squashed/stripped version of the name!
	 */
	public String getNpcName() {
		return mNpcName;
	}

	public void setNpcTrades(Plugin plugin, Villager villager, Player player) {
		// Get two copies of the current trades
		List<MerchantRecipe> origRecipes = new ArrayList<MerchantRecipe>(villager.getRecipes());
		List<MerchantRecipe> modifiedRecipes = new ArrayList<MerchantRecipe>(origRecipes);

		// Remove unmatched prereq trades
		boolean modified = false;
		String modifiedSlots = null;
		for (NpcTrade trade : mTrades) {
			if (!trade.prerequisiteMet(player)) {
				if (modifiedRecipes.size() <= trade.getIndex()) {
					player.sendMessage(ChatColor.RED + "BUG! This NPC has too few trades for some reason. Please report this!");
				} else {
					modifiedRecipes.remove(trade.getIndex());
					if (modifiedSlots == null) {
						modifiedSlots = Integer.toString(trade.getIndex());
					} else {
						modifiedSlots += ", " + Integer.toString(trade.getIndex());
					}

					modified = true;
				}
			}
		}

		if (modified) {
			if (player.getGameMode() == GameMode.CREATIVE && player.isOp()) {
				player.sendMessage(ChatColor.GOLD + "These trader slots were not shown to you: " + modifiedSlots);
				player.sendMessage(ChatColor.GOLD + "This message only appears to operators in creative mode");
			}

			villager.setRecipes(modifiedRecipes);

			villager.setMetadata(TRADER_MODIFIED_METAKEY, new FixedMetadataValue(plugin, 0));

			new BukkitRunnable() {
				@Override
				public void run() {
					if (!villager.isTrading()) {
						villager.removeMetadata(TRADER_MODIFIED_METAKEY, plugin);
						villager.setRecipes(origRecipes);
					}
				}
			}.runTaskTimer(plugin, 1, 1);
		}
	}
}
