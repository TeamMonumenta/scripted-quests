package com.playmonumenta.scriptedquests.api;

import com.floweytf.utils.stdstreams.IStandardByteReader;
import com.floweytf.utils.stdstreams.StandardByteReader;
import com.floweytf.utils.stdstreams.StandardByteWriter;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Constants;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class ClientChatProtocol implements PluginMessageListener, CommandExecutor {
	private static @MonotonicNonNull ClientChatProtocol INSTANCE = null;
	private static final String VERSION = "1.0";
	private static final Gson GSON = new Gson();
	private final Set<UUID> mShouldSendMessage = new HashSet<>();
	private boolean mOverride = false;

	private ClientChatProtocol(Plugin plugin) {
		plugin.getCommand("toggleclientchatapi").setExecutor(this);

		plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, Constants.API_CHANNEL_ID);
		plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, Constants.API_CHANNEL_ID, this);
	}

	public static ClientChatProtocol getInstance() {
		return INSTANCE;
	}

	public static void initialize(Plugin plugin) {
		INSTANCE = new ClientChatProtocol(plugin);
	}

	public void deinitialize() {
		Plugin plugin = Plugin.getInstance();
		plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin);
		plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin);
	}

	private static void sendJson(Player player, JsonObject object) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		StandardByteWriter out = new StandardByteWriter(stream);
		try {
			out.write(GSON.toJson(object));
			player.sendPluginMessage(Plugin.getInstance(), Constants.API_CHANNEL_ID, stream.toByteArray());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void sendPacket(List<QuestComponent> packet, QuestContext context) {
		// implements `S->C 'actions'`
		JsonObject data = JsonObjectBuilder.get()
			.add("type", "actions")
			.add("data", packet.stream().map(v -> v.serializeForClientAPI(context))
				.map(v -> v.orElse(null))
				.collect(Collectors.toList()))
			.build();

		sendJson(context.getPlayer(), data);
	}

	@Override
	public void onPluginMessageReceived(String s, Player player, byte[] bytes) {
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		IStandardByteReader out = new StandardByteReader(stream);
		try {
			String mode = out.readString();
			JsonObject request = GSON.fromJson(mode, JsonElement.class).getAsJsonObject();

			// implements `C->S 'version'` and `C->S 'mode'`
			switch (request.get("type").getAsString()) {
				case "version":
					// implements `S->C 'version'`
					sendJson(player, JsonObjectBuilder.get()
						.add("type", "version")
						.add("version", VERSION)
						.build()
					);
					break;
				case "mode":
					boolean newMode = request.get("send").getAsJsonPrimitive().getAsBoolean();
					if (newMode) {
						mShouldSendMessage.add(player.getUniqueId());
					} else {
						mShouldSendMessage.remove(player.getUniqueId());
					}
					break;
				default:
					break; // Silently ignore other type requests
			}
		} catch (Exception e) {
			e.printStackTrace();
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
