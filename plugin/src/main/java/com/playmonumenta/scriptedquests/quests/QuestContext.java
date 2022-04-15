package com.playmonumenta.scriptedquests.quests;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import java.util.LinkedList;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Holds context for a quest file execution. This will be created when a quest file starts executing, e.g. when interacting with an NPC, and is passed to all prerequisites and actions of the file.
 */
public class QuestContext {

	private final Plugin mPlugin;
	private final Player mPlayer;
	private final @Nullable Entity mNpcEntity;
	private final boolean mUseNpcForPrerequisites;
	private final @Nullable QuestPrerequisites mPrerequisites;
	private final @Nullable ItemStack mUsedItem;

	public QuestContext(Plugin plugin, Player player, @Nullable Entity npcEntity) {
		mPlugin = plugin;
		mPlayer = player;
		mUseNpcForPrerequisites = false;
		mNpcEntity = npcEntity;
		mPrerequisites = null;
		mUsedItem = null;
	}

	public QuestContext(Plugin plugin, Player player, @Nullable Entity npcEntity, boolean useNpc, @Nullable QuestPrerequisites prerequisites, @Nullable ItemStack usedItem) {
		mPlugin = plugin;
		mPlayer = player;
		mUseNpcForPrerequisites = useNpc;
		mNpcEntity = npcEntity;
		mPrerequisites = prerequisites;
		mUsedItem = usedItem;
	}

	public Plugin getPlugin() {
		return mPlugin;
	}

	/**
	 * Gets the player that this quest file is executed for.
	 */
	public Player getPlayer() {
		return mPlayer;
	}

	/**
	 * Gets the Entity to use for prerequisite checks. Should not be used outside of prerequisite checks.
	 */
	public Entity getEntityUsedForPrerequisites() {
		return mUseNpcForPrerequisites ? mNpcEntity : mPlayer;
	}

	/**
	 * Gets the interacted NPC entity, if any. Note that NPC quest files can be run even without an entity present.
	 */
	public @Nullable Entity getNpcEntity() {
		return mNpcEntity;
	}

	public QuestPrerequisites getPrerequisites() {
		return mPrerequisites;
	}

	/**
	 * Checks if the prerequisites are still met. Useful when an action is delayed and thus state may have changed.
	 * Returns true if there have not been any prerequisites defined or non are applicable for the current context.
	 */
	public boolean prerequisitesMet() {
		return mPrerequisites == null || mPrerequisites.prerequisiteMet(this);
	}

	/**
	 * Gets the item that was used in the event. Only valid for interactables, where the item is either the item used in the event (main or offhand),
	 * or the clicked item in an inventory for inventory click events.
	 */
	public @Nullable ItemStack getUsedItem() {
		return mUsedItem;
	}

	/**
	 * Returns a new {@link QuestContext} with the given prerequisites set so that {@link #prerequisitesMet()} will check them.
	 * If this context already had prerequisites, the given prerequisites will be merged with the existing ones.
	 * Passing in null will have no effect and return a clone of this context.
	 */
	public QuestContext withPrerequisites(@Nullable QuestPrerequisites prerequisites) {
		QuestPrerequisites newPrerequisites = prerequisites == null ? mPrerequisites : mPrerequisites == null ? prerequisites : mPrerequisites.union(prerequisites);
		return new QuestContext(mPlugin, mPlayer, mNpcEntity, mUseNpcForPrerequisites, newPrerequisites, mUsedItem);
	}

	public QuestContext clearPrerequisites() {
		return new QuestContext(mPlugin, mPlayer, mNpcEntity, mUseNpcForPrerequisites, null, mUsedItem);
	}

	/**
	 * Returns a new {@link QuestContext} that will return the NPC entity from {@link #getEntityUsedForPrerequisites()} (which returns the player by default).
	 */
	public QuestContext useNpcForPrerequisites(boolean useNpc) {
		return new QuestContext(mPlugin, mPlayer, mNpcEntity, useNpc, mPrerequisites, mUsedItem);
	}

	/**
	 * Returns a new {@link QuestContext} with the given NPC set (and {@link #useNpcForPrerequisites(boolean)} will be reset to false)
	 */
	public QuestContext withNpc(@Nullable Entity npcEntity) {
		return new QuestContext(mPlugin, mPlayer, npcEntity, false, mPrerequisites, mUsedItem);
	}

	// Static thread-local context storage for providing the context to executed commands (e.g. used by GUIs)
	private static final ThreadLocal<LinkedList<QuestContext>> CURRENT_CONTEXT = ThreadLocal.withInitial(LinkedList::new);

	/**
	 * Pushes the given {@link QuestContext} onto the static thread-local context stack so that the context is available to executed commands.
	 * <p>
	 * <b>{@link #popCurrentContext()} must be called to remove the context from the stack again!</b>
	 */
	public static void pushCurrentContext(QuestContext currentContext) {
		CURRENT_CONTEXT.get().push(currentContext);
	}

	/**
	 * Gets the current context from the static thread-local stack, i.e. gets the latest context pushed by {@link #pushCurrentContext(QuestContext) pushCurrentContext}.
	 */
	public static @Nullable QuestContext getCurrentContext() {
		return CURRENT_CONTEXT.get().peek();
	}

	public static void popCurrentContext() {
		CURRENT_CONTEXT.get().pop();
	}
}
