package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import me.Novalescent.Core;
import me.Novalescent.mobs.rpg.RPGMob;
import me.Novalescent.mobs.spells.scripted.actions.SpellActions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;

public class ActionSpawnMob implements ActionBase {

	private String mMobId;
	public SpellActions mActions;
	public final World mWorld;
	public final Location mLocation;

	public ActionSpawnMob(JsonElement value) throws Exception {
		JsonObject json = value.getAsJsonObject();

		mMobId = json.get("mob_id").getAsString();
		String worldname = json.get("world").getAsString();
		mWorld = Bukkit.getWorld(worldname);

		if (mWorld == null) {
			throw new Exception("world value is not a valid world!");
		}

		// Center
		double x = 0;
		double y = 0;
		double z = 0;
		if (json.get("location") == null ||
			json.get("location").getAsJsonObject() == null) {
			throw new Exception("Failed to parse 'center'");
		}

		JsonObject centerJson = json.get("location").getAsJsonObject();
		for (Map.Entry<String, JsonElement> ent : centerJson.entrySet()) {
			String key = ent.getKey();
			JsonElement v = ent.getValue();

			switch (key) {
				case "x":
					x = v.getAsDouble();
					break;

				case "y":
					y = v.getAsDouble();
					break;

				case "z":
					z = v.getAsDouble();
					break;

				default:
					throw new Exception("Unknown center key: '" + key + "'");
			}
		}

		mLocation = new Location(mWorld, x, y, z);

		mActions = new SpellActions(null, null, json.get("spell_actions"));
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		RPGMob mob = Core.getInstance().mMobManager.getMob(mMobId);

		if (mob != null) {
			LivingEntity entity = mob.spawnMob(mLocation);
			mActions.doActions(entity, mLocation.clone().add(0, 1, 0));
		}
	}
}
