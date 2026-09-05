package dev.kyluua.utilitiesscarce.module;

import dev.kyluua.utilitiesscarce.config.ConfigManager;
import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig;
import dev.kyluua.utilitiesscarce.util.ActionScheduler;
import dev.kyluua.utilitiesscarce.util.FreeCamState;
import dev.kyluua.utilitiesscarce.util.Notifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Detaches the camera from the body without moving the body.
 *
 * <p>The player entity is never touched: it keeps its position and, crucially,
 * its pitch and yaw. So if you were aiming down mining a shaft when you turned
 * Free Cam on, holding the attack key keeps breaking that same column while you
 * fly around looking at something else. Everything the game aims -- block
 * breaking, block placing, attacks, the block outline -- still comes off the
 * body, because vanilla ray-traces from the player and the player has not moved.
 *
 * <p>Two things have to be taken away from the body for that to hold. Movement
 * keys are cut off by swapping the player's input handler for a blank one, which
 * never reads the keyboard; the real handler is put back on exit. Mouse look is
 * intercepted in {@code MouseHandlerMixin} and applied to the camera instead.
 */
public final class FreeCamModule extends Module {
	private static final double MIN_MOTION = 1.0E-6D;

	private ClientInput savedInput;
	private LocalPlayer inputOwner;
	private float lastHealth = Float.MAX_VALUE;

	public FreeCamModule(ActionScheduler scheduler) {
		super("free_cam", scheduler);
	}

	@Override
	public boolean isEnabled() {
		return ConfigManager.get().freeCam.enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		ConfigManager.get().freeCam.enabled = enabled;
	}

	/** Free Cam has to keep running while the settings screen is open. */
	@Override
	public boolean runsWhileScreenOpen() {
		return true;
	}

	@Override
	public void onStop() {
		releaseInput();

		if (FreeCamState.isActive()) {
			FreeCamState.end();
		}

		lastHealth = Float.MAX_VALUE;
	}

	@Override
	public void onTick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;

		if (player == null) {
			onStop();
			return;
		}

		UtilitiesScarceConfig.FreeCam config = ConfigManager.get().freeCam;

		if (!FreeCamState.isActive()) {
			begin(player);
			return;
		}

		// Respawning or changing dimension hands us a new player object, whose
		// input handler still needs blanking.
		if (inputOwner != player) {
			releaseInput();
			captureInput(player);
		}

		float health = player.getHealth();

		if (config.disableOnDamage && health < lastHealth) {
			bailOut();
			return;
		}

		lastHealth = health;
		FreeCamState.beginTick();

		// Don't fly the camera around while the player is typing in chat.
		if (minecraft.screen != null) {
			return;
		}

		Vec3 motion = readMovement(minecraft.options, config);

		if (motion.lengthSqr() < MIN_MOTION) {
			return;
		}

		FreeCamState.setPosition(leash(player, FreeCamState.position().add(motion), config));
	}

	private void begin(LocalPlayer player) {
		FreeCamState.begin(new Vec3(player.getX(), player.getEyeY(), player.getZ()),
				player.getYRot(), player.getXRot());
		captureInput(player);
		lastHealth = player.getHealth();
	}

	/** Took a hit while away from the body: snap back rather than die blind. */
	private void bailOut() {
		setEnabled(false);
		onStop();
		ConfigManager.save();

		if (ConfigManager.get().general.notifyOnToggle) {
			Notifier.toggled(displayName(), false);
		}
	}

	/**
	 * Swaps in a blank input handler. The base class never polls the keyboard,
	 * so the body stops receiving movement while the real handler is parked.
	 */
	private void captureInput(LocalPlayer player) {
		savedInput = player.input;
		inputOwner = player;
		player.input = new ClientInput();
	}

	private void releaseInput() {
		if (inputOwner != null && savedInput != null) {
			inputOwner.input = savedInput;
		}

		inputOwner = null;
		savedInput = null;
	}

	private Vec3 readMovement(Options options, UtilitiesScarceConfig.FreeCam config) {
		// Horizontal movement comes off the player's own input handler rather
		// than off raw key checks, so it follows whatever the movement keys are
		// bound to and picks up anything else that feeds that handler: toggle
		// sneak, analog sticks from controller mods, and so on. The vector
		// counts left as positive, hence the flip.
		Vec2 move = movementInput();
		double forward = move.y;
		double strafe = -move.x;
		double vertical = axis(options.keyJump.isDown(), options.keyShift.isDown());

		if (forward == 0.0D && strafe == 0.0D && vertical == 0.0D) {
			return Vec3.ZERO;
		}

		float yawRad = FreeCamState.yaw() * Mth.DEG_TO_RAD;
		float pitchRad = FreeCamState.pitch() * Mth.DEG_TO_RAD;

		Vec3 forwardVector = config.followPitch
				? new Vec3(-Mth.sin(yawRad) * Mth.cos(pitchRad), -Mth.sin(pitchRad),
						Mth.cos(yawRad) * Mth.cos(pitchRad))
				: new Vec3(-Mth.sin(yawRad), 0.0D, Mth.cos(yawRad));

		Vec3 rightVector = new Vec3(-Mth.cos(yawRad), 0.0D, -Mth.sin(yawRad));
		Vec3 plane = forwardVector.scale(forward).add(rightVector.scale(strafe));

		// Clamp instead of normalising: a half-pressed stick should still move
		// at half speed, while a diagonal must not outrun a straight line.
		if (plane.lengthSqr() > 1.0D) {
			plane = plane.normalize();
		}

		Vec3 direction = plane.add(0.0D, vertical, 0.0D);

		if (direction.lengthSqr() < MIN_MOTION) {
			return Vec3.ZERO;
		}

		double speed = Math.max(0.0D, config.moveSpeed);

		if (options.keySprint.isDown()) {
			speed *= Math.max(1.0D, config.sprintMultiplier);
		}

		return direction.scale(speed);
	}

	/**
	 * Ticks the real input handler -- the one parked while the body carries a
	 * blank copy -- and reads the movement it produced.
	 */
	private Vec2 movementInput() {
		if (savedInput == null) {
			return Vec2.ZERO;
		}

		savedInput.tick();
		return savedInput.getMoveVector();
	}

	private static double axis(boolean positive, boolean negative) {
		return (positive ? 1.0D : 0.0D) - (negative ? 1.0D : 0.0D);
	}

	/** Keeps the camera within the configured radius of the body. */
	private static Vec3 leash(LocalPlayer player, Vec3 target, UtilitiesScarceConfig.FreeCam config) {
		if (config.maxDistance <= 0.0D) {
			return target;
		}

		Vec3 body = new Vec3(player.getX(), player.getEyeY(), player.getZ());
		Vec3 offset = target.subtract(body);
		double distance = offset.length();

		if (distance <= config.maxDistance) {
			return target;
		}

		return body.add(offset.normalize().scale(config.maxDistance));
	}
}
