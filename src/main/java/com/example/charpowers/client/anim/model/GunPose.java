package com.example.charpowers.client.anim.model;

// Where the minigun is this frame, in player-model pixels and turned with the view, so the gun drawn in the world and the hands
// on it always agree. One gun pixel is a sixteenth of a block; one model pixel is a sixteenth of 0.9375 of a block.
public final class GunPose {
    public static final double GUN_TO_MODEL = 1 / 0.9375;

    public final double[] position; // model pixels
    public final Rot turn;
    public final double size;       // model pixels per gun pixel

    private GunPose(double[] position, Rot turn, double size) {
        this.position = position;
        this.turn = turn;
        this.size = size;
    }

    // armY: how far down the shoulders are in the model. netYaw: how far you look away from where your legs point.
    public static GunPose of(GunNumbers n, double armY, double netYaw, double pitch, GunMotion motion) {
        double up = Math.toRadians(n.pitchUp), down = Math.toRadians(n.pitchDown);
        double tilt = pitch < up ? up : pitch > down ? down : pitch;
        Rot tip = Rot.x(tilt), yaw = Rot.y(netYaw);
        double[] from = tip.apply(n.origin[0] - n.pivot[0], n.origin[1] - n.pivot[1], n.origin[2] - n.pivot[2]);
        double[] at = yaw.apply(n.pivot[0] + from[0] + motion.dx, n.pivot[1] + from[1] + motion.dy, n.pivot[2] + from[2] + motion.dz);
        Rot turn = yaw.then(tip).then(Rot.euler(motion.rx, motion.ry, motion.rz));
        return new GunPose(new double[] {at[0], armY + at[1], at[2]}, turn, n.size * GUN_TO_MODEL);
    }

    // A point on the gun, in the gun model's own pixels, where it is on the player model now. In the gun's numbers x is to your
    // right, y up and z forward.
    public double[] point(double[] gun) {
        double[] p = turn.apply(-gun[0] * size, -gun[1] * size, -gun[2] * size);
        return new double[] {position[0] + p[0], position[1] + p[1], position[2] + p[2]};
    }
}
