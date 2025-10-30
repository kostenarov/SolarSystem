package io.kostenarov.SolarSystem.Classes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class CelestialBody {
    public final String name;
    public final float mass;
    public final float radius;
    public final Color color;

    public Vector2 position;
    public Vector2 velocity;
    public Vector2 acceleration = new Vector2();

    private final Array<Vector2> trail = new Array<>();

    public CelestialBody(String name, float mass, float radius, Color color, Vector2 position, Vector2 velocity) {
        this.name = name;
        this.mass = mass;
        this.radius = radius;
        this.color = color;
        this.position = position;
        this.velocity = velocity;
    }

    public String getName() { return name; }
    public float getMass() { return mass; }
    public float getRadius() { return radius; }
    public Color getColor() { return color; }
    public Vector2 getPosition() { return position; }
    public Vector2 getVelocity() { return velocity; }

    public Array<Vector2> getTrail() { return trail; }

    public void addTrailPoint(Vector2 pos, int maxPoints) {
        if (trail.size > 0) {
            Vector2 last = trail.peek();
            if (last.epsilonEquals(pos, 0.001f)) return;
        }
        trail.add(new Vector2(pos));
        if (trail.size > maxPoints) {
            trail.removeIndex(0);
        }
    }
}
