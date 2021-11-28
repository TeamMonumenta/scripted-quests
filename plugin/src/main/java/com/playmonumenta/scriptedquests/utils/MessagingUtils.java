package com.playmonumenta.scriptedquests.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.playmonumenta.scriptedquests.managers.TranslationsManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.scriptedquests.Plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;

public class MessagingUtils {
	private static final Pattern RE_NUMERIC = Pattern.compile("[0-9]+");
	public static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
	public static final LegacyComponentSerializer AMPERSAND_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
	public static final GsonComponentSerializer GSON_COMPONENT_SERIALIZER = GsonComponentSerializer.gson();
	public static final PlainComponentSerializer PLAIN_SERIALIZER = PlainComponentSerializer.plain();

	public static String translatePlayerName(Player player, String message) {
		return message.replaceAll("@S", player.getName()).replaceAll("@U", player.getUniqueId().toString().toLowerCase());
	}

	public static void sendActionBarMessage(Player player, String message) {
		message = TranslationsManager.translate(player, message);
		message = translatePlayerName(player, message);
		TextComponent formattedMessage = LEGACY_SERIALIZER.deserialize(message);
		formattedMessage = formattedMessage.color(NamedTextColor.YELLOW);
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
		formattedMessage = formattedMessage.color(color);
		if (bold) {
			formattedMessage = formattedMessage.decorate(TextDecoration.BOLD);
		}
		player.sendMessage(formattedMessage);
	}

	public static void sendNPCMessage(Player player, String displayName, String message) {
		message = TranslationsManager.translate(player, message);
		message = translatePlayerName(player, message);
		TextComponent formattedMessage = LEGACY_SERIALIZER.deserialize("[" + displayName + "] ");
		formattedMessage = formattedMessage.color(NamedTextColor.GOLD);
		TextComponent tempText = AMPERSAND_SERIALIZER.deserialize(message.replace("§", "&"));
		tempText = tempText.color(NamedTextColor.WHITE);
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
		formattedMessage = formattedMessage.color(NamedTextColor.GOLD);
		message = message.color(NamedTextColor.WHITE);
		formattedMessage = formattedMessage.append(message);

		player.sendMessage(formattedMessage);
	}

	public static void sendRawMessage(Player player, String message) {
		sendRawMessage(player, message, true);
	}

	public static void sendRawMessage(Player player, String message, boolean allowTranslations) {
		if (allowTranslations) {
			message = TranslationsManager.translate(player, message);
		}
		message = translatePlayerName(player, message);
		message = message.replace('§', '&');
		TextComponent formattedMessage = AMPERSAND_SERIALIZER.deserialize(message);
		player.sendMessage(formattedMessage);
	}

	public static void sendClickableNPCMessage(Plugin plugin, Player player, String message,
	                                           String commandStr, HoverEvent hoverEvent) {
		message = TranslationsManager.translate(player, message);
		message = translatePlayerName(player, message);
		Component formattedMessage = LEGACY_SERIALIZER.deserialize("[" + message + "]");
		formattedMessage = formattedMessage.color(NamedTextColor.LIGHT_PURPLE)
			.clickEvent(ClickEvent.runCommand(commandStr));

		if (hoverEvent != null) {
			formattedMessage = formattedMessage.hoverEvent(hoverEvent);
		}


		player.sendMessage(formattedMessage);
	}

	public static void sendStackTrace(CommandSender sender, Exception e) {
		Set<CommandSender> senders = new HashSet<CommandSender>();
		senders.add(sender);
		sendStackTrace(senders, e);
	}

	public static void sendStackTrace(Set<CommandSender> senders, Exception e) {
		TextComponent formattedMessage;
		String errorMessage = e.getLocalizedMessage();
		if (errorMessage != null) {
			formattedMessage = LEGACY_SERIALIZER.deserialize(errorMessage);
		} else {
			formattedMessage = Component.text("An error occured without a set message. Hover for stack trace.");
		}
		formattedMessage.color(NamedTextColor.RED);

		// Get the first 300 characters of the stacktrace and send them to the player
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String sStackTrace = sw.toString();
		sStackTrace = sStackTrace.substring(0, Math.min(sStackTrace.length(), 300));

		TextComponent textStackTrace = Component.text(sStackTrace.replace("\t", "  "), NamedTextColor.RED);
		formattedMessage.hoverEvent(textStackTrace);
		for (CommandSender sender : senders) {
			if (sender != null) {
				sender.sendMessage(formattedMessage);
			}
		}

		e.printStackTrace();
	}

	public static Component translate(Player player, Component message) {
		JsonElement messageJson = GSON_COMPONENT_SERIALIZER.serializeToTree(message);
		messageJson = translateJson(player, messageJson);
		return GSON_COMPONENT_SERIALIZER.deserializeFromTree(messageJson);
	}

	private static JsonElement translateJson(Player player, JsonElement message) {
		String messageStr;
		if (message instanceof JsonPrimitive) {
			JsonPrimitive messagePrimitive = (JsonPrimitive) message;
			messageStr = messagePrimitive.getAsString();
			if (messageStr.isEmpty()) {
				return messagePrimitive;
			} else if (RE_NUMERIC.matcher(messageStr).matches()) {
				return messagePrimitive;
			}
			return new JsonPrimitive(TranslationsManager.translate(player, messageStr));
		} else if (message instanceof JsonArray) {
			JsonArray messageArray = (JsonArray) message;
			int size = messageArray.size();
			for (int i = 0; i < size; ++i) {
				JsonElement childMessage = messageArray.get(i);
				childMessage = translateJson(player, childMessage);
				messageArray.set(i, childMessage);
			}
			return message;
		} else if (message instanceof JsonObject) {
			JsonObject messageObject = (JsonObject) message;
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
