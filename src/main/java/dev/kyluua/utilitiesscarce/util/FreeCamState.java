package dev.kyluua.utilitiesscarce.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Where the free camera is and where it is looking.
 *
 * <p>Held statically because the mixins that move the camera and steal the
 * mouse run far away from the module that drives them. Everything here is
 * touched from the client thread only.
 *
 * <p>The player's body is deliberately not represented: free cam never moves it,
 * which is what lets the body keep mining whatever it was aimed at.
 */
public final class FreeCamState {
	/** Same factor vanilla applies to raw mouse deltas. */
	private static final float MOUSE_SENSITIVITY_FACTOR = 0.15F;

	private static boolean active;
	private static Vec3 position = Vec3.ZERO;
	private static Vec3 previousPosition = Vec3.ZERO;
	private static float yaw;
	private static float pitch;

	private FreeCamState() {
	}

	public static boolean isActive() {
		return active;
	}

	public static float yaw() {
		return yaw;
	}

	public static float pitch() {
		return pitch;
	}

	public static Vec3 position() {
		return position;
	}

	/** Camera position interpolated between the last two ticks, for rendering. */
	public static Vec3 cameraPos(float partialTick) {
		float delta = Mth.clamp(partialTick, 0.0F, 1.0F);

		return new Vec3(
				previousPosition.x + (position.x - previousPosition.x) * delta,
				previousPosition.y + (position.y - previousPosition.y) * delta,
				previousPosition.z + (position.z - previousPosition.z) * delta);
	}

	public static void begin(Vec3 startPosition, float startYaw, float startPitch) {
		position = startPosition;
		previousPosition = startPosition;
		yaw = startYaw;
		pitch = startPitch;
		active = true;
	}

	public static void end() {
		active = false;
		position = Vec3.ZERO;
		previousPosition = Vec3.ZERO;
	}

	/** Marks the start of a tick, so rendering interpolates from here. */
	public static void beginTick() {
		previousPosition = position;
	}

	/** Moves the camera for this tick; rendering interpolates from beginTick. */
	public static void setPosition(Vec3 target) {
		position = target;
	}

	/**
	 * Applies a mouse movement to the camera instead of the player. Deltas
	 * arrive raw, scaled the same way vanilla scales them.
	 */
	public static void turn(double deltaYaw, double deltaPitch) {
		if (!active) {
			return;
		}

		yaw = Mth.wrapDegrees(yaw + (float) deltaYaw * MOUSE_SENSITIVITY_FACTOR);
		pitch = Mth.clamp(pitch + (float) deltaPitch * MOUSE_SENSITIVITY_FACTOR, -90.0F, 90.0F);
	}
}
