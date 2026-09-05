package dev.kyluua.utilitiesscarce.render;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Emits the line geometry the highlight modules draw. */
public final class RenderHelper {
	private RenderHelper() {
	}

	/**
	 * One line segment. The line shader wants a direction per vertex, so the
	 * normal is the segment's own direction.
	 */
	public static void line(PoseStack.Pose pose, VertexConsumer buffer, double x1, double y1, double z1,
			double x2, double y2, double z2, int color, float width) {
		float ax = (float) x1;
		float ay = (float) y1;
		float az = (float) z1;
		float bx = (float) x2;
		float by = (float) y2;
		float bz = (float) z2;

		Vector3f normal = new Vector3f(bx - ax, by - ay, bz - az);

		if (normal.lengthSquared() < 1.0E-9F) {
			return;
		}

		normal.normalize();

		buffer.addVertex(pose, ax, ay, az).setColor(color).setNormal(pose, normal).setLineWidth(width);
		buffer.addVertex(pose, bx, by, bz).setColor(color).setNormal(pose, normal).setLineWidth(width);
	}

	public static void line(PoseStack.Pose pose, VertexConsumer buffer, Vec3 from, Vec3 to, int color,
			float width) {
		line(pose, buffer, from.x, from.y, from.z, to.x, to.y, to.z, color, width);
	}

	/** Unit vector for a yaw/pitch pair, in Minecraft's degree convention. */
	public static Vec3 direction(float yaw, float pitch) {
		float yawRad = yaw * Mth.DEG_TO_RAD;
		float pitchRad = pitch * Mth.DEG_TO_RAD;

		return new Vec3(-Mth.sin(yawRad) * Mth.cos(pitchRad), -Mth.sin(pitchRad),
				Mth.cos(yawRad) * Mth.cos(pitchRad));
	}

	/** The twelve edges of a box. */
	public static void box(PoseStack.Pose pose, VertexConsumer buffer, AABB box, int color, float width) {
		double x1 = box.minX;
		double y1 = box.minY;
		double z1 = box.minZ;
		double x2 = box.maxX;
		double y2 = box.maxY;
		double z2 = box.maxZ;

		// Bottom face.
		line(pose, buffer, x1, y1, z1, x2, y1, z1, color, width);
		line(pose, buffer, x2, y1, z1, x2, y1, z2, color, width);
		line(pose, buffer, x2, y1, z2, x1, y1, z2, color, width);
		line(pose, buffer, x1, y1, z2, x1, y1, z1, color, width);

		// Top face.
		line(pose, buffer, x1, y2, z1, x2, y2, z1, color, width);
		line(pose, buffer, x2, y2, z1, x2, y2, z2, color, width);
		line(pose, buffer, x2, y2, z2, x1, y2, z2, color, width);
		line(pose, buffer, x1, y2, z2, x1, y2, z1, color, width);

		// Uprights.
		line(pose, buffer, x1, y1, z1, x1, y2, z1, color, width);
		line(pose, buffer, x2, y1, z1, x2, y2, z1, color, width);
		line(pose, buffer, x2, y1, z2, x2, y2, z2, color, width);
		line(pose, buffer, x1, y1, z2, x1, y2, z2, color, width);
	}
}
