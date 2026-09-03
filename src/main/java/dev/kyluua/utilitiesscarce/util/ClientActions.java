package dev.kyluua.utilitiesscarce.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Wrappers around the interactions the modules perform.
 *
 * <p>Attacks and block uses issued from here are flagged as synthetic so the
 * modules can ignore the interaction events their own actions raise, which
 * would otherwise make a swap sequence trigger itself forever.
 */
public final class ClientActions {
	private static boolean synthetic;

	private ClientActions() {
	}

	/** True while an interaction started by this mod is being dispatched. */
	public static boolean isSynthetic() {
		return synthetic;
	}

	private static void runSynthetic(Runnable action) {
		boolean previous = synthetic;
		synthetic = true;

		try {
			action.run();
		} finally {
			synthetic = previous;
		}
	}

	/**
	 * Attacks a target with the currently selected item. The game flushes any
	 * pending held-item change before sending the attack, so selecting a slot
	 * and attacking in the same tick still lands with the new item.
	 */
	public static void attack(Minecraft minecraft, Entity target) {
		LocalPlayer player = minecraft.player;

		if (player == null || minecraft.gameMode == null || target == null) {
			return;
		}

		runSynthetic(() -> {
			minecraft.gameMode.attack(player, target);
			player.swing(InteractionHand.MAIN_HAND);
		});
	}

	/** Right-clicks a block face with the currently selected item. */
	public static void useItemOn(Minecraft minecraft, BlockHitResult hitResult) {
		LocalPlayer player = minecraft.player;

		if (player == null || minecraft.gameMode == null || hitResult == null) {
			return;
		}

		runSynthetic(() -> {
			InteractionResult result =
					minecraft.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);

			// Vanilla swings only when the interaction asks for it. Swinging
			// regardless costs a packet per use and animates clicks that the
			// server refused.
			if (result instanceof InteractionResult.Success success
					&& success.swingSource() == InteractionResult.SwingSource.CLIENT) {
				player.swing(InteractionHand.MAIN_HAND);
			}
		});
	}

	/** Whether the attack cooldown has fully recharged. */
	public static boolean attackReady(LocalPlayer player) {
		return player.getAttackStrengthScale(0.0F) >= 1.0F;
	}

	/** Whether the target is still a legitimate thing to hit. */
	public static boolean isValidTarget(LocalPlayer player, Entity target, double maxRange) {
		if (target == null || !target.isAlive() || target.isRemoved() || target == player) {
			return false;
		}

		return player.distanceToSqr(target) <= maxRange * maxRange;
	}
}
