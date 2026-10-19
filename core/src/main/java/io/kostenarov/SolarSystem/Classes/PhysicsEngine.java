package io.kostenarov.SolarSystem.Classes;

import com.badlogic.gdx.utils.Array;

public class PhysicsEngine {

    public static final double G = 6.67430e-11;

    public enum Integrator {
        // First-order: one force evaluation per step, velocity updated before position
        SEMI_IMPLICIT_EULER("Euler"),
        // Second-order (kick-drift-kick leapfrog): much smaller energy drift for the same step size
        VELOCITY_VERLET("Verlet");

        public final String label;

        Integrator(String label) {
            this.label = label;
        }
    }

    private final Array<CelestialBody> bodies;
    private double maxSubstep;
    private Integrator integrator;

    // Verlet reuses the accelerations computed at the end of its previous step
    private boolean accelerationsValid = false;

    public PhysicsEngine(Array<CelestialBody> bodies) {
        this(bodies, Integrator.VELOCITY_VERLET, Double.POSITIVE_INFINITY);
    }

    /**
     * @param maxSubstep longest single integration step in seconds; each update is split into
     *                   equal substeps no longer than this, so fast inner moons stay stable
     */
    public PhysicsEngine(Array<CelestialBody> bodies, Integrator integrator, double maxSubstep) {
        this.bodies = bodies;
        this.integrator = integrator;
        this.maxSubstep = maxSubstep;
    }

    public Integrator getIntegrator() {
        return integrator;
    }

    public void setIntegrator(Integrator integrator) {
        this.integrator = integrator;
        accelerationsValid = false;
    }

    public void setMaxSubstep(double maxSubstep) {
        this.maxSubstep = maxSubstep;
    }

    public void update(double deltaTime) {
        if (deltaTime > 0) {
            int substeps = Math.max(1, (int) Math.ceil(deltaTime / maxSubstep));
            double h = deltaTime / substeps;
            for (int s = 0; s < substeps; s++) {
                if (integrator == Integrator.VELOCITY_VERLET) {
                    stepVelocityVerlet(h);
                } else {
                    stepSemiImplicitEuler(h);
                }
            }
        }

        for (CelestialBody body : bodies) {
            body.syncFloatState();
        }
    }

    private void stepSemiImplicitEuler(double h) {
        computeAccelerations();
        for (CelestialBody body : bodies) {
            body.vx += body.ax * h;
            body.vy += body.ay * h;
            body.x += body.vx * h;
            body.y += body.vy * h;
        }
        // Positions moved, so the accelerations no longer match them
        accelerationsValid = false;
    }

    private void stepVelocityVerlet(double h) {
        if (!accelerationsValid) computeAccelerations();
        double halfH = h / 2;

        for (CelestialBody body : bodies) {
            body.vx += body.ax * halfH;
            body.vy += body.ay * halfH;
            body.x += body.vx * h;
            body.y += body.vy * h;
        }

        computeAccelerations();

        for (CelestialBody body : bodies) {
            body.vx += body.ax * halfH;
            body.vy += body.ay * halfH;
        }
    }

    private void computeAccelerations() {
        for (CelestialBody body : bodies) {
            body.ax = 0;
            body.ay = 0;
        }

        // Each pair once: by Newton's third law the pull on B is equal and opposite to the pull on A
        for (int i = 0; i < bodies.size; i++) {
            CelestialBody a = bodies.get(i);
            for (int j = i + 1; j < bodies.size; j++) {
                CelestialBody b = bodies.get(j);

                double dx = b.x - a.x;
                double dy = b.y - a.y;
                double distanceSquared = dx * dx + dy * dy;
                if (distanceSquared == 0) continue;

                // |a| = G * m / r^2 along the unit vector (dx, dy) / r
                double scale = G / (distanceSquared * Math.sqrt(distanceSquared));
                a.ax += scale * b.mass * dx;
                a.ay += scale * b.mass * dy;
                b.ax -= scale * a.mass * dx;
                b.ay -= scale * a.mass * dy;
            }
        }
        accelerationsValid = true;
    }

    //Runge kutta
    //adams bashforth
    //ppn method
    //3d vector
    //accurate poses - jpl horizons
}
