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
	public double mX = 0;
	public double mY = 0;
	public double mZ = 0;

	public ActionSpawnMob(JsonElement value) throws Exception {
		JsonObject json = value.getAsJsonObject();

		mMobId = json.get("mob_id").getAsString();
		String worldname = json.get("world").getAsString();
		mWorld = Bukkit.getWorld(worldname);

		// Center
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
					mX = v.getAsDouble();
					break;

				case "y":
					mY = v.getAsDouble();
					break;

				case "z":
					mZ = v.getAsDouble();
					break;

				default:
					throw new Exception("Unknown center key: '" + key + "'");
			}
		}

		mActions = new SpellActions(null, null, json.get("spell_actions"));
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		RPGMob mob = Core.getInstance().mMobManager.getMob(mMobId);

		if (mob != null) {
			Location loc = new Location(mWorld != null ? mWorld : player.getWorld(), mX, mY, mZ);
			LivingEntity entity = mob.spawnMob(loc);
			mActions.doActions(entity, loc.clone().add(0, 1, 0));
		}
	}
}
