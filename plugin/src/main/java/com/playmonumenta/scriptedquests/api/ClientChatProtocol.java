package com.playmonumenta.scriptedquests.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.floweytf.utils.stdstreams.IStandardByteReader;
import com.floweytf.utils.stdstreams.StandardByteReader;
import com.floweytf.utils.stdstreams.StandardByteWriter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class ClientChatProtocol implements PluginMessageListener, CommandExecutor {
	private static final Gson GSON = new Gson();
	private final Set<UUID> mShouldSendMessage = new HashSet<>();
	private boolean mOverride = false;
	private static ClientChatProtocol INSTANCE = null;

	public ClientChatProtocol() {
		INSTANCE = this;
	}

	public static void sendPacket(List<QuestComponent> packet, Plugin plugin, Player player, Entity npc) {
		JsonObject data = JsonObjectBuilder.get()
			.add("type", "actions")
			.add("data", packet.stream().map(v -> v.serializeForClientAPI(plugin, player, npc))
				.map(v -> v.orElse(null))
				.collect(Collectors.toList()))
			.build();

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		StandardByteWriter out = new StandardByteWriter(stream);
		try {
			out.write(GSON.toJson(data));
			player.sendPluginMessage(Plugin.getInstance(), Constants.API_CHANNEL_ID, stream.toByteArray());
		}
		catch (Exception e) {
			// should never throw
		}
	}

	@Override
	public void onPluginMessageReceived(String s, Player player, byte[] bytes) {
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		IStandardByteReader out = new StandardByteReader(stream);
		try {
			String mode = out.readString();
			if (mode.equals("enabled")) {
				mShouldSendMessage.add(player.getUniqueId());
			} else if (mode.equals("disabled")) {
				mShouldSendMessage.remove(player.getUniqueId());
			}
		}
		catch (Exception e) {
			// should never throw (again)
		}
	}

	public static boolean shouldSend(Player p) {
		if (INSTANCE != null) {
			return INSTANCE.mOverride || INSTANCE.mShouldSendMessage.contains(p.getUniqueId());
		}
		return false;
	}

	@Override
	public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
		mOverride = !mOverride;
		Plugin.getInstance().getLogger().info("Should always send custom data to player: " + mOverride);
		return true;
	}
}
