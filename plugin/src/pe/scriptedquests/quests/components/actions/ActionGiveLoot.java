package pe.scriptedquests.quests;

import com.google.gson.JsonElement;

import java.util.List;
import java.util.Random;

import net.minecraft.server.v1_12_R1.EntityItem;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.ItemStack;
import net.minecraft.server.v1_12_R1.LootTable;
import net.minecraft.server.v1_12_R1.LootTableInfo;
import net.minecraft.server.v1_12_R1.MinecraftKey;
import net.minecraft.server.v1_12_R1.World;
import net.minecraft.server.v1_12_R1.WorldServer;

import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import pe.scriptedquests.Plugin;

class ActionGiveLoot implements ActionBase {
	private String mLootPath;

	ActionGiveLoot(JsonElement element) throws Exception {
		mLootPath = element.getAsString();
		if (mLootPath == null) {
			throw new Exception("Command value is not a string!");
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, QuestPrerequisites prereqs) {
		World nmsWorld = ((CraftWorld)player.getWorld()).getHandle();
		EntityPlayer nmsPlayer = ((CraftPlayer)player).getHandle();

		// Generate items from the specified loot table
		LootTable lootTable = nmsWorld.getLootTableRegistry().a(new MinecraftKey(mLootPath));
		List<ItemStack> loot = lootTable.a(new Random(),
		                                   new LootTableInfo(0, (WorldServer)nmsWorld,
		                                                     nmsWorld.getLootTableRegistry(),
		                                                     null, null, null));

		// Give those items to the player (this code based on AdvancementRewards.java)
		for (ItemStack itemstack : loot) {
			EntityItem entityitem = nmsPlayer.drop(itemstack, false);

			if (entityitem != null) {
				entityitem.r();
				entityitem.d(nmsPlayer.getName());
			}
		}
	}
}
