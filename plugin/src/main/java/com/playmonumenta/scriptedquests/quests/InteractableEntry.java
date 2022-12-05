package com.playmonumenta.scriptedquests.quests;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.components.QuestComponent;
import com.playmonumenta.scriptedquests.utils.JsonUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class InteractableEntry {
	public enum InteractType {
		RIGHT_CLICK_AIR,
		RIGHT_CLICK_BLOCK,
		RIGHT_CLICK_ENTITY,
		RIGHT_CLICK_FUNCTIONAL,
		RIGHT_CLICK_INVENTORY,
		LEFT_CLICK_AIR,
		LEFT_CLICK_BLOCK,
		LEFT_CLICK_ENTITY,
		LEFT_CLICK_INVENTORY,
		SWAP_HANDS;

		public static InteractType fromString(String str) throws Exception {
			switch (str) {
				case "right_click_air":
					return RIGHT_CLICK_AIR;
				case "right_click_block":
					return RIGHT_CLICK_BLOCK;
				case "right_click_entity":
					return RIGHT_CLICK_ENTITY;
				case "right_click_functional":
					return RIGHT_CLICK_FUNCTIONAL;
				case "right_click_inventory":
					return RIGHT_CLICK_INVENTORY;
				case "left_click_air":
					return LEFT_CLICK_AIR;
				case "left_click_block":
					return LEFT_CLICK_BLOCK;
				case "left_click_entity":
					return LEFT_CLICK_ENTITY;
				case "left_click_inventory":
					return LEFT_CLICK_INVENTORY;
				case "swap_hands":
					return SWAP_HANDS;
				default:
					throw new Exception("Unknown click_type: " + str);
			}
		}
	}

	private final ArrayList<QuestComponent> mComponents = new ArrayList<>();
	private final EnumSet<InteractType> mInteractTypes = EnumSet.noneOf(InteractType.class);
	private final ImmutableSet<Material> mMaterials;
	private final boolean mCancelEvent;
	private final boolean mAllowItemOnCooldown;

	public InteractableEntry(JsonObject object) throws Exception {
		//////////////////////////////////////// material (Required) ////////////////////////////////////////
		JsonElement material = object.get("material");
		if (material == null) {
			throw new Exception("'material' entry is required");
		}
		if (material.isJsonArray()) {
			ImmutableSet.Builder<Material> matSetBuilder = ImmutableSet.builder();
			for (JsonElement matElement : material.getAsJsonArray()) {
				Material mat = Material.getMaterial(matElement.getAsString());
				if (mat == null) {
					throw new Exception("Material not found: " + matElement.getAsString());
				}
				matSetBuilder.add(mat);
			}
			mMaterials = matSetBuilder.build();
		} else if (material.isJsonPrimitive() && material.getAsJsonPrimitive().isString()) {
			Material mat = Material.getMaterial(material.getAsString());
			if (mat == null) {
				throw new Exception("Material not found: " + material.getAsString());
			}
			mMaterials = ImmutableSet.of(mat);
		} else {
			throw new Exception("Failed to parse 'material' as string or array");
		}

		//////////////////////////////////////// clicks (Required) ////////////////////////////////////////
		JsonElement clicks = object.get("click_types");
		if (clicks == null) {
			throw new Exception("'click_types' entry is required");
		}
		if (!clicks.isJsonArray()) {
			throw new Exception("Failed to parse 'click_types' as array");
		}
		for (JsonElement element : clicks.getAsJsonArray()) {
			mInteractTypes.add(InteractType.fromString(element.getAsString()));
		}

		//////////////////////////////////////// quest_components (Required) ////////////////////////////////////////
		JsonElement questComponents = object.get("quest_components");
		if (questComponents == null) {
			throw new Exception("'quest_components' entry is required");
		}
		JsonArray array = questComponents.getAsJsonArray();
		if (array == null) {
			throw new Exception("Failed to parse 'quest_components' as JSON array");
		}

		for (JsonElement entry : array) {
			mComponents.add(new QuestComponent("", "", EntityType.PLAYER, entry));
		}

		//////////////////////////////////////// cancel_event (Optional) ////////////////////////////////////////
		// Read the optional cancel_event value - default to false (do not cancel)
		JsonElement cancelEvent = object.get("cancel_event");
		mCancelEvent = cancelEvent != null && cancelEvent.getAsBoolean();

		//////////////////////////////////////// allow_item_on_cooldown (Optional) ////////////////////////////////////////
		mAllowItemOnCooldown = JsonUtils.getBoolean(object, "allow_item_on_cooldown", false);

		//////////////////////////////////////// Fail if other keys exist ////////////////////////////////////////
		Set<Map.Entry<String, JsonElement>> entries = object.entrySet();
		for (Map.Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("material") && !key.equals("click_types") && !key.equals("cancel_event")
				    && !key.equals("quest_components") && !key.equals("allow_item_on_cooldown")) {
				throw new Exception("Unknown quest key: " + key);
			}
		}
	}

	public ArrayList<QuestComponent> getComponents() {
		return mComponents;
	}

	public ImmutableSet<Material> getMaterials() {
		return mMaterials;
	}

	public boolean interactEvent(QuestContext context, InteractType interactType) {
		if (!mAllowItemOnCooldown
			    && context.getUsedItem() != null
			    && !context.getUsedItem().getType().isAir()
			    && context.getPlayer().getCooldown(context.getUsedItem().getType()) > 0) {
			return false;
		}
		boolean cancelEvent = false;
		if (mInteractTypes.contains(interactType)) {
			for (QuestComponent component : mComponents) {
				if (component.doActionsIfPrereqsMet(context)) {
					cancelEvent = mCancelEvent;
				}
			}
		}
		return cancelEvent;
	}
}
