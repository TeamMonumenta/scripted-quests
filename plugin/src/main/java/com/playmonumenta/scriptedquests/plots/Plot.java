package com.playmonumenta.scriptedquests.plots;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.utils.JsonUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

public class Plot {
	private PlotStyleVariant mStyleVariant;
	private final Vector mCorner;
	// Lock the plot so no one may enter
	private boolean mLocked = false;
	// Full permissions, including ability to grant and transfer ownership.
	private UUID mOwnerId;
	// Full control over plot contents, may invite guests.
	private Set<UUID> mCoOwnerIds = new HashSet<UUID>();
	// Guest permissions.
	private Map<UUID, PlotPermissions> mGuests = new HashMap<UUID, PlotPermissions>();
	// Public permissions.
	private PlotPermissions mPublicPerms = new PlotPermissions();

	/***********************************
	 * Constructors and json
	 ***********************************/

	protected Plot(PlotStyleVariant styleVariant, UUID ownerId, Vector insideLoc) {
		mStyleVariant = styleVariant;
		mOwnerId = ownerId;
		mCorner = insideLoc.clone().subtract(mStyleVariant.mInsideLoc.toVector());
	}

	protected Plot(CommandSender sender, JsonObject object) throws Exception {
		if (object == null) {
			throw new Exception("object may not be null.");
		}

		String style = null;
		String variant = null;
		Vector insideLoc = null;
		Set<UUID> coOwnerIds = new HashSet<UUID>();

		for (Map.Entry<String, JsonElement> ent : object.entrySet()) {
			String key = ent.getKey();
			JsonElement value = ent.getValue();

			switch (key) {
			case "style":
				style = value.getAsString();
				break;
			case "variant":
				variant = value.getAsString();
				break;
			case "inside_location":
				insideLoc = JsonUtils.getAsVector(value, "inside_location");
				break;
			case "owner":
				String ownerUuidStr = value.getAsString();
				mOwnerId = UUID.fromString(ownerUuidStr);
				break;
			case "co_owners":
				if (!value.isJsonArray()) {
					throw new Exception("Failed to parse 'co_owners' as array.");
				}
				for (JsonElement coOwnerJson : value.getAsJsonArray()) {
					String coOwnerUuidStr = coOwnerJson.getAsString();
					if (coOwnerUuidStr == null) {
						throw new Exception("Co-owners must be UUID strings.");
					}
					coOwnerIds.add(UUID.fromString(coOwnerUuidStr));
				}
				break;
			case "guests":
				guestsFromJson(sender, value.getAsJsonObject());
				break;
			case "public":
				mPublicPerms = new PlotPermissions(sender, value.getAsJsonObject());
				break;
			default:
				throw new Exception("Unknown Plot key: " + key);
			}
		}

		if (style == null) {
			throw new Exception("Plot style must not be missing or null.");
		}
		if (variant == null) {
			throw new Exception("Plot variant must not be missing or null.");
		}
		if (insideLoc == null) {
			throw new Exception("Plot inside_location must not be missing or null.");
		}
		// owner may be skipped
		// co_owners may be skipped
		// guests may be skipped
		// public may be skipped

		mStyleVariant = PlotManager.getVariant(style, variant);
		if (mStyleVariant == null) {
			throw new Exception("Could not find plot style/variant '" + style + "', '" + variant + "'.");
		}

		mCorner = insideLoc.clone().subtract(mStyleVariant.mInsideLoc.toVector());
	}

	private void guestsFromJson(CommandSender sender, JsonObject object) throws Exception {
		if (object == null) {
			throw new Exception("Expected guests to be type object.");
		}

		for (Map.Entry<String, JsonElement> guestEnt : object.entrySet()) {
			String guestUuidStr = guestEnt.getKey();
			JsonElement guestElement = guestEnt.getValue();
			JsonObject guestDetails = guestElement.getAsJsonObject();

			UUID guestId = UUID.fromString(guestUuidStr);
			PlotPermissions perm = new PlotPermissions(sender, guestDetails);
			mGuests.put(guestId, perm);
		}
	}

	protected JsonObject toJson() {
		Vector insideLocVec = getWorldLoc(mStyleVariant.mInsideLoc.toVector());
		JsonObject insideLocJson = JsonUtils.toJsonObject(insideLocVec);

		JsonArray coOwnersJson = new JsonArray();
		for (UUID coOwner : mCoOwnerIds) {
			coOwnersJson.add(coOwner.toString());
		}

		JsonObject guestsJson = new JsonObject();
		for (Map.Entry<UUID, PlotPermissions> guestEnt : mGuests.entrySet()) {
			UUID guestId = guestEnt.getKey();
			PlotPermissions guestPerms = guestEnt.getValue();
			JsonObject guestPermsJson = guestPerms.toJson();
			guestsJson.add(guestId.toString(), guestPermsJson);
		}

		JsonObject publicPermsJson = mPublicPerms.toJson();

		JsonObject result = new JsonObject();
		result.add("inside_location", insideLocJson);
		result.addProperty("style", mStyleVariant.getPlotStyle().getName());
		result.addProperty("variant", mStyleVariant.getName());
		result.addProperty("owner", mOwnerId.toString());
		result.add("co_owners", coOwnersJson);
		result.add("guests", guestsJson);
		result.add("public", publicPermsJson);
		return result;
	}

	/***********************************
	 * Location and teleportation
	 ***********************************/

	public Vector getMin() {
		return mCorner.clone();
	}

	public Vector getMax() {
		return mCorner.clone().add(mStyleVariant.mSize);
	}

	private Location getPlotLoc(Location worldLoc) {
		Location result = worldLoc.clone();
		result.subtract(mCorner);
		return result;
	}

	private Vector getPlotLoc(Vector worldLoc) {
		Vector result = worldLoc.clone();
		result.subtract(mCorner);
		return result;
	}

	private Location getWorldLoc(Location plotLoc) {
		Location result = plotLoc.clone();
		result.add(mCorner);
		return result;
	}

	private Vector getWorldLoc(Vector plotLoc) {
		Vector result = plotLoc.clone();
		result.add(mCorner);
		return result;
	}

	public Location getInsideLoc() {
		return getWorldLoc(mStyleVariant.mInsideLoc);
	}

	public Location getOutsideLoc() {
		return getWorldLoc(mStyleVariant.mOutsideLoc);
	}

	public void tpInside(Player player) {
		tpInside(player, false);
	}

	public void tpInside(Player player, boolean preserveFacing) {
		tpToLoc(player, getInsideLoc(), preserveFacing);
	}

	public void tpOutside(Player player) {
		tpOutside(player, false);
	}

	public void tpOutside(Player player, boolean preserveFacing) {
		tpToLoc(player, getOutsideLoc(), preserveFacing);
	}

	private void tpToLoc(Player player, Location destination, boolean preserveFacing) {
		Location playerLoc = player.getLocation().clone();

		destination.setWorld(playerLoc.getWorld());
		if (preserveFacing) {
			destination.setYaw(playerLoc.getYaw());
			destination.setPitch(playerLoc.getPitch());
		}

		player.teleport(destination);
	}

	/***********************************
	 * Plot style/variant
	 ***********************************/

	public PlotStyle getPlotStyle() {
		return mStyleVariant.getPlotStyle();
	}

	public PlotStyleVariant getPlotVariant() {
		return mStyleVariant;
	}

	/***********************************
	 * Claiming/Abandoning (Owner)
	 ***********************************/

	public boolean isClaimed() {
		return (mOwnerId == null);
	}

	public boolean claim(CommandSender sender) {
		if (sender == null) {
			return false;
		}

		if (!(sender instanceof Player)) {
			MessagingUtils.sendCommandError(sender, "This command must be run as a player.");
			return false;
		}
		Player newOwner = (Player) sender;

		if (mLocked && isClaimed() && isOwner(newOwner)) {
			mLocked = false;
			MessagingUtils.sendCommandSuccess(sender, "Welcome home.");
			return true;
		}

		if (isClaimed()) {
			MessagingUtils.sendCommandError(sender, "This plot is already claimed.");
			return false;
		}

		mOwnerId = newOwner.getUniqueId();
		MessagingUtils.sendCommandSuccess(sender, "Welcome to your new plot.");
		return true;
	}

	// For the owner
	public boolean lock(CommandSender sender) {
		if (sender == null) {
			return false;
		}

		if (!(sender instanceof Player)) {
			MessagingUtils.sendCommandError(sender, "This command must be run as a player.");
			return false;
		}
		Player owner = (Player) sender;

		if (!isOwner(owner)) {
			MessagingUtils.sendCommandError(sender, "Only the plot owner may run this.");
			return false;
		}

		if (mLocked) {
			MessagingUtils.sendCommandError(sender, "Your plot is already locked.");
			return false;
		}

		mLocked = false;
		MessagingUtils.sendCommandSuccess(sender, "Your plot is now locked.");
		return true;
	}

	// For the owner
	public boolean reset(CommandSender sender) {
		if (sender == null) {
			return false;
		}

		if (!(sender instanceof Player)) {
			MessagingUtils.sendCommandError(sender, "This command must be run as a player.");
			return false;
		}
		Player owner = (Player) sender;

		if (!isOwner(owner)) {
			MessagingUtils.sendCommandError(sender, "Only the plot owner may run this.");
			return false;
		}

		if (!mCoOwnerIds.isEmpty()) {
			MessagingUtils.sendCommandError(sender, "You may not reset your plot until all co-owners are removed. You may also transfer ownership to a co-owner.");
			return false;
		}

		mGuests.clear();
		mPublicPerms.clear();
		mOwnerId = null;
		MessagingUtils.sendCommandSuccess(sender, "Your plot has been reset.");
		return true;
	}

	/***********************************
	 * Owner methods
	 ***********************************/

	public boolean isOwner(OfflinePlayer target) {
		if (target == null) {
			return false;
		}
		return target.getUniqueId().equals(mOwnerId);
	}

	public UUID getOwnerId() {
		return mOwnerId;
	}

	public OfflinePlayer getOwner() {
		if (mOwnerId == null) {
			return null;
		}
		return Bukkit.getOfflinePlayer​(mOwnerId);
	}

	public boolean transferOwnership(CommandSender sender, OfflinePlayer newOwner) {
		boolean isOwner = ((sender instanceof Player) && isOwner((Player) sender));
		if (!isOwner && !sender.isOp()) {
			MessagingUtils.sendCommandError(sender, "Only the owner and operators may transfer plot ownership.");
			return false;
		}

		if (newOwner == null) {
			MessagingUtils.sendCommandError(sender, "Could not identify the new owner.");
			return false;
		}
		UUID newOwnerId = newOwner.getUniqueId();
		if (isOwner(newOwner)) {
			if (newOwner.getName() == null) {
				MessagingUtils.sendCommandError(sender, "That player already owns this plot.");
			} else {
				MessagingUtils.sendCommandError(sender, newOwner.getName() + " already owns this plot.");
			}
			return false;
		}

		PlotManager plotManager = PlotManager.getPlotManager(null);
		if (plotManager.getPlayerPlot(newOwner, mStyleVariant.getPlotStyle().getName()) != null) {
			if (newOwner.getName() == null) {
				MessagingUtils.sendCommandError(sender, "That player already owns a plot of this style.");
			} else {
				MessagingUtils.sendCommandError(sender, newOwner.getName() + " already owns a plot of this style.");
			}
			return false;
		}

		// Move the old owner into the co-owners list.
		mCoOwnerIds.add(mOwnerId);

		// Remove the new owner from co-owner/guest lists.
		mCoOwnerIds.remove(newOwnerId);
		mGuests.remove(newOwnerId);

		// Change ownership to new owner.
		mOwnerId = newOwnerId;

		MessagingUtils.sendCommandSuccess(sender, "Successfully transfered ownership.");
		return true;
	}

	/***********************************
	 * Co-Owner methods
	 ***********************************/

	public boolean isCoOwner(OfflinePlayer target) {
		if (target == null) {
			return false;
		}
		UUID targetId = target.getUniqueId();
		return mCoOwnerIds.contains(targetId);
	}

	public Set<OfflinePlayer> getCoOwners() {
		Set<OfflinePlayer> result = new HashSet<OfflinePlayer>();
		for (UUID coOwnerId : mCoOwnerIds) {
			result.add(Bukkit.getOfflinePlayer​(coOwnerId));
		}
		return result;
	}

	public boolean addCoOwner(CommandSender sender, OfflinePlayer newCoOwner) {
		if (sender == null || newCoOwner == null) {
			return false;
		}

		if (!sender.isOp() && !(sender instanceof Player) && !isOwner((Player) sender)) {
			MessagingUtils.sendCommandError(sender, "You may not do this unless you are the owner or an operator.");
			return false;
		}

		if (isOwner(newCoOwner)) {
			if (newCoOwner.getName() == null) {
				MessagingUtils.sendCommandError(sender, "That player is currently this plot's owner.");
			} else {
				MessagingUtils.sendCommandError(sender, newCoOwner.getName() + " is currently this plot's owner.");
			}
			return false;
		}

		if (isCoOwner(newCoOwner)) {
			if (newCoOwner.getName() == null) {
				MessagingUtils.sendCommandError(sender, "That player is already a co-owner.");
			} else {
				MessagingUtils.sendCommandError(sender, newCoOwner.getName() + " is already a co-owner.");
			}
			return false;
		}

		UUID newCoOwnerId = newCoOwner.getUniqueId();
		mGuests.remove(newCoOwnerId);
		mCoOwnerIds.add(newCoOwnerId);
		if (newCoOwner.getName() == null) {
			MessagingUtils.sendCommandSuccess(sender, "Added that player as a co-owner.");
		} else {
			MessagingUtils.sendCommandSuccess(sender, "Added " + newCoOwner.getName() + " as a co-owner.");
		}
		return true;
	}

	public boolean removeCoOwner(CommandSender sender, OfflinePlayer coOwner) {
		if (sender == null || coOwner == null) {
			return false;
		}

		if (!sender.isOp() && !(sender instanceof Player) && !isOwner((Player) sender)) {
			MessagingUtils.sendCommandError(sender, "You may not do this unless you are the owner or an operator.");
			return false;
		}

		if (!isCoOwner(coOwner)) {
			if (coOwner.getName() == null) {
				MessagingUtils.sendCommandError(sender, "That player is not a co-owner.");
			} else {
				MessagingUtils.sendCommandError(sender, coOwner.getName() + " is not a co-owner.");
			}
			return false;
		}

		UUID coOwnerId = coOwner.getUniqueId();
		mCoOwnerIds.remove(coOwnerId);
		if (coOwner.getName() == null) {
			MessagingUtils.sendCommandSuccess(sender, "That player is no longer a co-owner.");
		} else {
			MessagingUtils.sendCommandSuccess(sender, coOwner.getName() + " is no longer a co-owner.");
		}
		return true;
	}

	public boolean removeAllCoOwners(CommandSender sender) {
		if (sender == null) {
			return false;
		}

		if (!sender.isOp() && !(sender instanceof Player) && !isOwner((Player) sender)) {
			MessagingUtils.sendCommandError(sender, "You may not do this unless you are the owner or an operator.");
			return false;
		}

		mCoOwnerIds.clear();
		MessagingUtils.sendCommandSuccess(sender, "Removed all co-owners.");
		return true;
	}

	/***********************************
	 * Guest methods
	 ***********************************/

	public boolean isGuest(OfflinePlayer target) {
		if (target == null) {
			return false;
		}
		UUID targetId = target.getUniqueId();
		return mGuests.containsKey(targetId);
	}

	public Set<OfflinePlayer> getGuests() {
		Set<OfflinePlayer> result = new HashSet<OfflinePlayer>();
		for (UUID guestId : mGuests.keySet()) {
			result.add(Bukkit.getOfflinePlayer​(guestId));
		}
		return result;
	}

	public boolean addGuest(CommandSender sender, OfflinePlayer target) {
		if (sender == null || target == null) {
			return false;
		}
		UUID targetId = target.getUniqueId();

		if (!sender.isOp() && !(sender instanceof Player) && !isOwner((Player) sender) && !isCoOwner((Player) sender)) {
			MessagingUtils.sendCommandError(sender, "You must be a co-owner, the owner, or an operator to do this.");
			return false;
		}

		if (isOwner(target) || isCoOwner(target) || isGuest(target)) {
			if (target.getName() == null) {
				MessagingUtils.sendCommandError(sender, "That player already has access to this plot.");
			} else {
				MessagingUtils.sendCommandError(sender, target.getName() + " already has access to this plot.");
			}
			return false;
		}

		// Add access
		mGuests.put(targetId, new PlotPermissions(mPublicPerms));
		if (target.getName() == null) {
			MessagingUtils.sendCommandSuccess(sender, "Added that player as a guest.");
		} else {
			MessagingUtils.sendCommandSuccess(sender, "Added " + target.getName() + " as a guest.");
		}
		return true;
	}

	public boolean removeGuest(CommandSender sender, OfflinePlayer target) {
		if (sender == null || target == null) {
			return false;
		}
		UUID targetId = target.getUniqueId();

		if (!sender.isOp() && !(sender instanceof Player) && !isOwner((Player) sender) && !isCoOwner((Player) sender)) {
			MessagingUtils.sendCommandError(sender, "You must be a co-owner, the owner, or an operator to do this.");
			return false;
		}

		if (isOwner(target)) {
			if (target.getName() == null) {
				MessagingUtils.sendCommandError(sender, "That player is the owner of this plot.");
			} else {
				MessagingUtils.sendCommandError(sender, target.getName() + " is the owner of this plot.");
			}
			return false;
		}

		if (isCoOwner(target)) {
			if (target.getName() == null) {
				MessagingUtils.sendCommandError(sender, "That player is a co-owner of this plot.");
			} else {
				MessagingUtils.sendCommandError(sender, target.getName() + " is a co-owner of this plot.");
			}
			return false;
		}

		if (!isGuest(target)) {
			if (target.getName() == null) {
				MessagingUtils.sendCommandError(sender, "That player is not a guest.");
			} else {
				MessagingUtils.sendCommandError(sender, target.getName() + " is not a guest.");
			}
			return false;
		}

		// Remove access
		mGuests.remove(targetId);
		if (target.getName() == null) {
			MessagingUtils.sendCommandSuccess(sender, "That player is no longer a guest.");
		} else {
			MessagingUtils.sendCommandSuccess(sender, target.getName() + " is no longer a guest.");
		}
		return true;
	}

	public boolean removeAllGuests(CommandSender sender) {
		if (sender == null) {
			return false;
		}

		if (!sender.isOp() && !(sender instanceof Player) && !isOwner((Player) sender) && !isCoOwner((Player) sender)) {
			MessagingUtils.sendCommandError(sender, "You must be a co-owner, the owner, or an operator to do this.");
			return false;
		}

		// Remove access
		mGuests.clear();
		MessagingUtils.sendCommandSuccess(sender, "Removed all guests.");
		return true;
	}

	public PlotPermissions getGuestPerms(OfflinePlayer target) {
		if (target == null) {
			return null;
		}
		return new PlotPermissions(mGuests.get(target.getUniqueId()));
	}

	/***********************************
	 * Co-Owner/Guest methods
	 ***********************************/

	// For everyone but the owner
	public boolean leave(CommandSender sender) {
		if (sender == null) {
			return false;
		}

		if (!(sender instanceof Player)) {
			MessagingUtils.sendCommandError(sender, "This command must be run as a player.");
			return false;
		}
		Player target = (Player) sender;

		if (isOwner(target)) {
			MessagingUtils.sendCommandError(sender, "This command may not be run as the owner. Consider transferring ownership, locking the plot, or resetting the plot");
			return false;
		}

		if (!isCoOwner(target) && !isGuest(target)) {
			MessagingUtils.sendCommandError(sender, "You are not the owner or a guest.");
		}

		UUID targetId = target.getUniqueId();
		mCoOwnerIds.remove(targetId);
		mGuests.remove(targetId);
		MessagingUtils.sendCommandSuccess(sender, "You have given up access to this plot.");
		return true;
	}

	/***********************************
	 * Public methods - err, public
	 * access to the plot? A plot that
	 * is public? Look, I mean this
	 * applies to unlisted players,
	 * not Java-public.
	 ***********************************/

	public PlotPermissions getPublicPerms() {
		return new PlotPermissions(mPublicPerms);
	}

	/***********************************
	 * Check permissions
	 ***********************************/

	public boolean hasPermission(OfflinePlayer target, PlotPermissions.PermissionType perm) {
		if (target == null) {
			return false;
		}

		if (mLocked) {
			return false;
		}

		UUID targetId = target.getUniqueId();
		if (isOwner(target) || isCoOwner(target)) {
			return true;
		} else if (isGuest(target)) {
			PlotPermissions perms = getGuestPerms(target);
			if (perms.hasExpired()) {
				mGuests.remove(targetId);
				// Continue to public permissions.
			} else {
				return perms.hasPerm(perm);
			}
		}

		return getPublicPerms().hasPerm(perm);
	}
}
