package com.playmonumenta.scriptedquests.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.managers.TranslationsManager;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class MessagingUtils {
	private static final Pattern RE_NUMERIC = Pattern.compile("[0-9]+");
	public static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
	public static final LegacyComponentSerializer AMPERSAND_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
	public static final GsonComponentSerializer GSON_COMPONENT_SERIALIZER = GsonComponentSerializer.gson();
	public static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

	public static String plainText(Component formattedText) {
		// This is only legacy text because we have a bunch of section symbols lying around that need to be updated.
		String legacyText = PLAIN_SERIALIZER.serialize(formattedText);
		return plainFromLegacy(legacyText);
	}

	public static String plainFromLegacy(String legacyText) {
		return PLAIN_SERIALIZER.serialize(LEGACY_SERIALIZER.deserialize(legacyText));
	}

	public static String translatePlayerName(Player player, String message) {
		return message.replaceAll("@S", player.getName()).replaceAll("@U", player.getUniqueId().toString().toLowerCase());
	}

	public static void sendActionBarMessage(Player player, String message) {
		message = TranslationsManager.translate(player, message);
		message = translatePlayerName(player, message);
		TextComponent formattedMessage = LEGACY_SERIALIZER.deserialize(message);
		formattedMessage = Component.empty().color(NamedTextColor.YELLOW).append(formattedMessage);
		player.sendActionBar(formattedMessage);
	}

	public static void sendActionBarMessage(Player player, NamedTextColor color, boolean bold, String message) {
		sendActionBarMessage(player, color, bold, message, true);
	}

	public static void sendActionBarMessage(Player player, NamedTextColor color, boolean bold, String message, boolean allowTranslations) {
		if (allowTranslations) {
			message = TranslationsManager.translate(player, message);
		}
		message = translatePlayerName(player, message);
		Component formattedMessage = LEGACY_SERIALIZER.deserialize(message);
		formattedMessage = Component.empty().color(color).append(formattedMessage);
		if (bold) {
			formattedMessage = formattedMessage.decorate(TextDecoration.BOLD);
		}
		player.sendMessage(formattedMessage);
	}

	public static void sendNPCMessage(Player player, String displayName, String message) {
		message = TranslationsManager.translate(player, message);
		message = translatePlayerName(player, message);
		TextComponent formattedMessage = LEGACY_SERIALIZER.deserialize("[" + displayName + "] ");
		formattedMessage = Component.empty().color(NamedTextColor.GOLD).append(formattedMessage);
		TextComponent tempText = AMPERSAND_SERIALIZER.deserialize(message.replace("§", "&"));
		tempText = Component.empty().color(NamedTextColor.WHITE).append(tempText);
		formattedMessage = formattedMessage.append(tempText);

		player.sendMessage(formattedMessage);
	}

	public static void sendNPCMessage(Player player, String displayName, Component message) {
		displayName = TranslationsManager.translate(player, displayName);
		if (message instanceof TextComponent) {
			/* TODO: This should probably loop over all the text in the component - hover, etc. */
			String contentStr = ((TextComponent) message).content();
			contentStr = TranslationsManager.translate(player, contentStr);
			message = ((TextComponent) message).content(contentStr);
		}
		TextComponent formattedMessage = LEGACY_SERIALIZER.deserialize("[" + displayName + "] ");
		formattedMessage = Component.empty().color(NamedTextColor.GOLD).append(formattedMessage);
		message = Component.empty().color(NamedTextColor.WHITE).append(message);
		formattedMessage = formattedMessage.append(message);

		player.sendMessage(formattedMessage);
	}

	public static void sendRawMessage(Player player, String message) {
		sendRawMessage(player, message, true);
	}

	public static void sendRawMessage(final Player player,final String message,final boolean allowTranslations) {
		player.sendMessage(serializeRawMessage(player, message, allowTranslations));
	}

	public static TextComponent serializeRawMessage(final Player player,final String message,final boolean allowTranslations) {
		if (allowTranslations) message = TranslationsManager.translate(player, message);
		message = translatePlayerName(player, message);
		message = message.replace('§', '&');
		return AMPERSAND_SERIALIZER.deserialize(message);
	}

	public static void sendMessageSync(CommandSender sender, Component message) {
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> sender.sendMessage(message));
	}

	public static void sendMessageSync(CommandSender sender, String message) {
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> sender.sendMessage(message));
	}

	public static void sendClickableNPCMessage(Player player, String message,
	                                           String commandStr, @Nullable HoverEvent<?> hoverEvent) {
		message = TranslationsManager.translate(player, message);
		message = translatePlayerName(player, message);
		Component formattedMessage = LEGACY_SERIALIZER.deserialize("[" + message + "]");
		formattedMessage = Component.empty().color(NamedTextColor.LIGHT_PURPLE).append(formattedMessage)
			.clickEvent(ClickEvent.runCommand(commandStr));

		if (hoverEvent != null) {
			formattedMessage = formattedMessage.hoverEvent(hoverEvent);
		}


		player.sendMessage(formattedMessage);
	}

	public static void sendStackTrace(Audience audience, Exception e) {
		TextComponent formattedMessage;
		String errorMessage = e.getLocalizedMessage();
		if (errorMessage != null) {
			formattedMessage = LEGACY_SERIALIZER.deserialize(errorMessage);
		} else {
			formattedMessage = Component.text("An error occurred without a set message. Hover for stack trace.");
		}
		formattedMessage = Component.empty().color(NamedTextColor.RED).append(formattedMessage);

		// Get the first 300 characters of the stacktrace and send them to the player
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String sStackTrace = sw.toString();
		sStackTrace = sStackTrace.substring(0, Math.min(sStackTrace.length(), 300));

		TextComponent textStackTrace = Component.text(sStackTrace.replace("\t", "  "), NamedTextColor.RED);
		formattedMessage = formattedMessage.hoverEvent(textStackTrace);
		audience.sendMessage(formattedMessage);

		e.printStackTrace();
	}

	public static Component translate(Player player, Component message) {
		JsonElement messageJson = GSON_COMPONENT_SERIALIZER.serializeToTree(message);
		messageJson = translateJson(player, messageJson);
		return GSON_COMPONENT_SERIALIZER.deserializeFromTree(messageJson);
	}

	private static JsonElement translateJson(Player player, JsonElement message) {
		String messageStr;
		if (message instanceof JsonPrimitive messagePrimitive) {
			messageStr = messagePrimitive.getAsString();
			if (messageStr.isEmpty()) {
				return messagePrimitive;
			} else if (RE_NUMERIC.matcher(messageStr).matches()) {
				return messagePrimitive;
			}
			return new JsonPrimitive(TranslationsManager.translate(player, messageStr));
		} else if (message instanceof JsonArray messageArray) {
			int size = messageArray.size();
			for (int i = 0; i < size; ++i) {
				JsonElement childMessage = messageArray.get(i);
				childMessage = translateJson(player, childMessage);
				messageArray.set(i, childMessage);
			}
			return message;
		} else if (message instanceof JsonObject messageObject) {
			JsonObject hoverEvent = messageObject.getAsJsonObject("hoverEvent");
			if (hoverEvent != null) {
				JsonPrimitive actionPrimitive = hoverEvent.getAsJsonPrimitive("action");
				String action = "UNKNOWN";
				if (actionPrimitive != null) {
					action = actionPrimitive.getAsString();
				}
				if (action.equals("show_entity")) {
					JsonObject showEntityContents = hoverEvent.getAsJsonObject("contents");
					if (showEntityContents != null) {
						JsonPrimitive entityTypePrimitive = showEntityContents.getAsJsonPrimitive("type");
						String entityTypeStr = "UNKNOWN";
						if (entityTypePrimitive != null) {
							entityTypeStr = entityTypePrimitive.getAsString();
						}
						if (entityTypeStr.equals("minecraft:player")) {
							// Don't translate player names (assuming vanilla selectors)
							return message;
						}

						JsonElement entityName = showEntityContents.get("name");
						if (entityName != null) {
							// Do translate other entity names
							entityName = translateJson(player, entityName);
							showEntityContents.add("name", entityName);
						}
					}
				} else if (action.equals("show_text")) {
					JsonElement showTextContents = hoverEvent.get("contents");
					showTextContents = translateJson(player, showTextContents);
					hoverEvent.add("contents", showTextContents);
				}
			}

			JsonArray extraArray = messageObject.getAsJsonArray("extra");
			if (extraArray != null) {
				messageObject.add("extra", translateJson(player, extraArray));
			}

			JsonPrimitive textPrimitive = messageObject.getAsJsonPrimitive("text");
			if (textPrimitive != null) {
				messageObject.add("text", translateJson(player, textPrimitive));
			}
			return message;
		} else {
			return message;
		}
	}
}
