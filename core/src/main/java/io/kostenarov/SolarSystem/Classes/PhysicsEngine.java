package io.kostenarov.SolarSystem.Classes;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class PhysicsEngine {

    public static final double G = 6.67430e-11;

    private final Array<CelestialBody> bodies;

    public PhysicsEngine(Array<CelestialBody> bodies) {
        this.bodies = bodies;
    }

    public void update(float deltaTime) {
        for (int i = 0; i < bodies.size; i++) {
            CelestialBody bodyA = bodies.get(i);
            bodyA.acceleration.set(0, 0);
            for (int j = 0; j < bodies.size; j++) {
                if (i == j) {
                    continue;
                }
                CelestialBody bodyB = bodies.get(j);

                Vector2 distanceVector = bodyB.position.cpy().sub(bodyA.position);
                float distanceSquared = distanceVector.len2();

                if (distanceSquared == 0) continue;

                // Newton's Law of Universal Gravitation: F = G * (m1*m2) / r^2
                double forceMagnitude = (G * bodyA.mass * bodyB.mass) / distanceSquared;

                Vector2 forceVector = distanceVector.nor().scl((float) forceMagnitude);

                // Second Law of Motion: Since F = m2 * a2 => a2 = F / m2 => The acceleration of body A is:
                // a2 = G * m1/r^2
                Vector2 accelerationDelta = forceVector.scl(1.0f / bodyA.mass);
                bodyA.acceleration.add(accelerationDelta);
            }
        }

        for (CelestialBody body : bodies) {
            body.velocity.mulAdd(body.acceleration, deltaTime);
            body.position.mulAdd(body.velocity, deltaTime);
        }
    }

    //Runge kutta
    //adams bashforth
    //ppn method
    //3d vector
    //accurate poses - jpl horizons
}

