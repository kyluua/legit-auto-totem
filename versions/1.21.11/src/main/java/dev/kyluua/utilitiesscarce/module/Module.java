package dev.kyluua.utilitiesscarce.module;

import dev.kyluua.utilitiesscarce.config.ConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import dev.kyluua.utilitiesscarce.util.ActionScheduler;
import dev.kyluua.utilitiesscarce.util.Notifier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;

/**
 * One toggleable feature. Enabled state lives in the config file so the hotkey
 * and the settings screen always agree.
 */
public abstract class Module {
	/**
	 * Scheduler owner shared by every module that takes over the held item.
	 * Submitting under one key means a new sequence cancels the previous one
	 * instead of two modules fighting over the hotbar.
	 */
	public static final String HAND_LANE = "hand";

	private final String id;
	protected final ActionScheduler scheduler;

	protected Module(String id, ActionScheduler scheduler) {
		this.id = id;
		this.scheduler = scheduler;
	}

	public final String id() {
		return id;
	}

	public final Component displayName() {
		return Component.translatable("text.utilitiesscarce.module." + id);
	}

	public abstract boolean isEnabled();

	public abstract void setEnabled(boolean enabled);

	/** Called once per client tick while the player is in a world. */
	public void onTick(Minecraft minecraft) {
	}

	/**
	 * Whether this module keeps ticking while a screen is open, regardless of
	 * the global pause setting. Only Free Cam needs it: giving up the camera
	 * the moment you open chat would be worse than useless.
	 */
	public boolean runsWhileScreenOpen() {
		return false;
	}

	/**
	 * Called when the player attacks an entity.
	 *
	 * @return {@code true} if this module started a sequence and therefore owns
	 *         the held item; lower-priority modules are then skipped
	 */
	public boolean onAttackEntity(Minecraft minecraft, Entity target) {
		return false;
	}

	/**
	 * Called while the level is being drawn, for modules that render. Runs on
	 * the render thread's pass, so read state rather than mutating it.
	 */
	public void onRender(WorldRenderContext context) {
	}

	/** Called when the player right-clicks a block. */
	public void onUseBlock(Minecraft minecraft, InteractionHand hand, BlockHitResult hitResult) {
	}

	/** Called when the module is switched off or the world goes away. */
	public void onStop() {
	}

	protected void announce(Component message) {
		if (ConfigManager.get().general.notifyOnAction) {
			Notifier.send(message);
		}
	}
}
