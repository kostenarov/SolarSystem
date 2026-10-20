package io.kostenarov.SolarSystem.physics;

import io.kostenarov.SolarSystem.SimulationSettings;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

public class CelestialBody {
    public final String name;
    public final float mass;
    public final float radius;
    public final Color color;
    public boolean hasRings = false;
    public boolean isMoon = false;
    /** For a moon, the body it was placed in orbit around; null otherwise. */
    public CelestialBody orbitParent;

    // Float copies of the simulation state, used for rendering and UI. Kept in sync by PhysicsEngine.
    public Vector2 position;
    public Vector2 velocity;

    // Simulation state in double precision: a float cannot resolve positions finer than ~16 km at 1 AU
    double x, y;
    double vx, vy;
    double ax, ay;

    private final Trail trail = new Trail(SimulationSettings.DEFAULT_TRAIL_LENGTH);

    public CelestialBody(String name, float mass, float radius, Color color, Vector2 position, Vector2 velocity, boolean hasRings, boolean isMoon) {
        this.name = name;
        this.mass = mass;
        this.radius = radius;
        this.color = color;
        this.position = position;
        this.velocity = velocity;
        this.hasRings = hasRings;
        this.isMoon = isMoon;
        this.x = position.x;
        this.y = position.y;
        this.vx = velocity.x;
        this.vy = velocity.y;
    }

    public CelestialBody(String name, float mass, float radius, Color color, Vector2 position, Vector2 velocity) {
        this(name, mass, radius, color, position, velocity, false, false);
    }

    /** Copies the body's properties and current state, without its trail. */
    public CelestialBody(CelestialBody other) {
        this(other.name, other.mass, other.radius, other.color, new Vector2(other.position), new Vector2(other.velocity), other.hasRings, other.isMoon);
        this.x = other.x;
        this.y = other.y;
        this.vx = other.vx;
        this.vy = other.vy;
    }

    public String getName() { return name; }
    public float getMass() { return mass; }
    public float getRadius() { return radius; }
    public Color getColor() { return color; }
    public Vector2 getPosition() { return position; }
    public Vector2 getVelocity() { return velocity; }
    public boolean hasRings() { return  hasRings; }
    public Trail getTrail() { return trail; }

    void syncFloatState() {
        position.set((float) x, (float) y);
        velocity.set((float) vx, (float) vy);
    }

    public void addTrailPoint(Vector2 pos, int maxPoints) {
        trail.setMaxPoints(maxPoints);
        trail.add(pos.x, pos.y);
    }
}
