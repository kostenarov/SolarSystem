package io.kostenarov.SolarSystem.physics;

import com.badlogic.gdx.math.Vector2;

/** A body's current two-body orbit around a reference body, from its relative position and velocity. */
public final class OrbitInfo {
    /** Distance from the reference body, m. */
    public final double distance;
    /** Speed relative to the reference body, m/s. */
    public final double speed;
    /** False if the body is moving fast enough to escape the reference body. */
    public final boolean bound;
    /** Semi-major axis, m; meaningful only when bound. */
    public final double semiMajorAxis;
    public final double eccentricity;
    /** Orbital period, s; meaningful only when bound. */
    public final double period;

    private OrbitInfo(double distance, double speed, boolean bound, double semiMajorAxis, double eccentricity, double period) {
        this.distance = distance;
        this.speed = speed;
        this.bound = bound;
        this.semiMajorAxis = semiMajorAxis;
        this.eccentricity = eccentricity;
        this.period = period;
    }

    public static OrbitInfo of(CelestialBody body, CelestialBody reference) {
        Vector2 p = body.getPosition(), q = reference.getPosition();
        Vector2 v = body.getVelocity(), w = reference.getVelocity();
        double rx = (double) p.x - q.x, ry = (double) p.y - q.y;
        double vx = (double) v.x - w.x, vy = (double) v.y - w.y;

        double r = Math.hypot(rx, ry);
        double speed = Math.hypot(vx, vy);
        double mu = PhysicalConstants.G * ((double) body.getMass() + reference.getMass());
        if (r == 0) return new OrbitInfo(0, speed, false, 0, 0, 0);

        // Specific orbital energy and angular momentum give the orbit's shape (vis-viva)
        double energy = speed * speed / 2 - mu / r;
        double angularMomentum = rx * vy - ry * vx;
        double eccentricity = Math.sqrt(Math.max(0, 1 + 2 * energy * angularMomentum * angularMomentum / (mu * mu)));

        if (energy >= 0) return new OrbitInfo(r, speed, false, Double.POSITIVE_INFINITY, eccentricity, Double.POSITIVE_INFINITY);
        double a = -mu / (2 * energy);
        double period = 2 * Math.PI * Math.sqrt(a * a * a / mu);
        return new OrbitInfo(r, speed, true, a, eccentricity, period);
    }
}
