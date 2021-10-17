package com.playmonumenta.scriptedquests.api;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionBase;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Protocol implements PluginMessageListener {
	private static final Gson GSON = new Gson();
	private final Set<UUID> mShouldSendMessage = new HashSet<>();

	public void sendPacket(List<QuestComponent> packet, Plugin plugin, Player player, Entity npc) {
		JsonObject obj = JsonObjectBuilder.get()
			.add("type", "actions")
			.add("data", packet.stream().map(v -> v.serialize(plugin, player, npc))
				.map(v -> v.orElse(null))
				.collect(Collectors.toList()))
			.build();

		// Logger logger = Plugin.getInstance().getLogger();
		// logger.info(GSON.toJson(obj));

		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF(GSON.toJson(obj));
		player.getServer().sendPluginMessage(Plugin.getInstance(), Constants.API_CHANNEL_ID, out.toByteArray());
	}

	@Override
	public void onPluginMessageReceived(@NotNull String s, @NotNull Player player, byte[] bytes) {
		ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
		String mode = in.readUTF();

		if(mode.equals("enabled")) {
			mShouldSendMessage.add(player.getUniqueId());
		} else if(mode.equals("disabled")) {
			mShouldSendMessage.remove(player.getUniqueId());
		}
	}

	public boolean shouldSend(Player p) {
		return mShouldSendMessage.contains(p.getUniqueId());
	}
}
