package com.playmonumenta.scriptedquests.plots;

import java.util.Calendar;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.command.CommandSender;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.utils.DateUtils;

public class PlotPermissions {
	public enum PermissionType {
		ENTER,    // Enter a plot via teleporter or front door.
		ITEMS,    // Move or take items; giving items always allowed. Includes item frames and armor stands.
		ANIMALS,  // Interact with animals - pigs, cows, etc.
		REDSTONE, // May interact with redstone; does not allow building/breaking redstone.
		CROPS,    // Place and break crops.
		BUILD;    // Place and break blocks, including those locked by other permissions.

		public static PermissionType fromString(String str) throws Exception {
			switch (str) {
				case "enter":
					return ENTER;
				case "items":
					return ITEMS;
				case "animals":
					return ANIMALS;
				case "redstone":
					return REDSTONE;
				case "crops":
					return CROPS;
				case "build":
					return BUILD;
				default:
					throw new Exception("Unknown permission: " + str);
			}
		}

		public String toString() {
			switch (this) {
				case ENTER:
					return "enter";
				case ITEMS:
					return "items";
				case ANIMALS:
					return "animals";
				case REDSTONE:
					return "redstone";
				case CROPS:
					return "crops";
				case BUILD:
					return "build";
				default:
					return null;
			}
		}
	}

	// Timestamp in ms since epoch from Calendar, or null if permissions do not expire
	private Long mExpireTimestamp = null;
	private EnumSet<PermissionType> mPerms = EnumSet.noneOf(PermissionType.class);

	protected PlotPermissions() {}

	protected PlotPermissions(PlotPermissions other) {
		mExpireTimestamp = other.mExpireTimestamp;
		mPerms = other.mPerms.clone();
	}

	protected PlotPermissions(CommandSender sender, JsonObject object) throws Exception {
		if (object == null) {
			throw new Exception("Plot permission object may not be null.");
		}

		// If "expires" is not set, guest permissions don't expire.
		JsonElement expiresJson = object.get("expires");
		if (expiresJson != null) {
			try {
				mExpireTimestamp = expiresJson.getAsLong();
			} catch (Exception e) {
				throw new Exception("Expected guest expires to be a Long (ms since epoch).");
			}
		}

		// This should be set, but if not, assume the permissions list was empty and omitted to save space.
		JsonArray permsJson = object.getAsJsonArray("permissions");
		if (permsJson != null) {
			for (JsonElement permJson : permsJson) {
				mPerms.add(PermissionType.fromString(permJson.toString()));
			}
		}
	}

	public JsonObject toJson() {
		JsonArray permsJson = new JsonArray();
		for (PermissionType perm : mPerms) {
			permsJson.add(perm.toString());
		}

		JsonObject result = new JsonObject();
		result.addProperty("expires", mExpireTimestamp);
		result.add("permissions", permsJson);

		return result;
	}

	public Calendar getExpireCalendar() {
		return DateUtils.getCalendarFromTimestamp(mExpireTimestamp);
	}

	public Long getExpireTimestamp() {
		return mExpireTimestamp;
	}

	public boolean hasExpired() {
		if (mExpireTimestamp == null) {
			return false;
		}
		if (DateUtils.getTimestamp() >= mExpireTimestamp) {
			mPerms.clear();
			mExpireTimestamp = null;
			return true;
		} else {
			return false;
		}
	}

	public void makePersist() {
		mExpireTimestamp = null;
	}

	public void setExpireTimestamp(Long timestamp) {
		mExpireTimestamp = timestamp;
	}

	public boolean hasPerm(PermissionType perm) {
		if (perm == null) {
			return false;
		}
		return !hasExpired() && mPerms.contains(perm);
	}

	public void givePerm(PermissionType perm) {
		if (perm != null) {
			mPerms.add(perm);
		}
	}

	public void takePerm(PermissionType perm) {
		mPerms.remove(perm);
	}

	public void clear() {
		mExpireTimestamp = null;
		mPerms.clear();
	}

	public String toString() {
		return mPerms.toString(); // TODO add expiration indication
	}
}
