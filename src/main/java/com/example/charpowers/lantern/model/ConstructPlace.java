package com.example.charpowers.lantern.model;

// Where each hand's model sits compared to its entity at the moment it hits (worked out from the animations in
// Blockbench). Spawning the entity at "target minus this" puts the hand on the target when it lands.
// right and forward are blocks along the way the entity faces; up is the height of the hand's middle.
public final class ConstructPlace {
    public record Offset(double right, double forward, double up) {}

    public static final Offset SLAP = new Offset(0.49, 0.57, 0.56), SLAM = new Offset(-0.74, -0.49, -1.06), FIST = new Offset(0.5, -0.37, 0.0),
            GRAB = new Offset(-0.32, 0.35, 0.27), HOLD = new Offset(-0.09, 0.43, 0.09);

    private ConstructPlace() {}

    // Facing yaw (Minecraft degrees) to the horizontal "forward" and "right" the entity's model uses.
    public static double[] forward(float yaw) {
        double r = Math.toRadians(yaw);
        return new double[] {-Math.sin(r), Math.cos(r)};
    }

    public static double[] right(float yaw) {
        double r = Math.toRadians(yaw);
        return new double[] {-Math.cos(r), -Math.sin(r)};
    }

    // Where to put the entity so the model's hit spot lands on (x, z) at height y (the target's middle for hands that
    // meet a body, the ground for a slam or a fist).
    public static double[] spawn(Offset o, float yaw, double x, double y, double z) {
        double[] f = forward(yaw), r = right(yaw);
        return new double[] {x - r[0] * o.right() - f[0] * o.forward(), y - o.up(), z - r[1] * o.right() - f[1] * o.forward()};
    }
}
