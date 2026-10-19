package io.kostenarov.SolarSystem.Classes;

/**
 * One body as stored in a save file. Planets and stars use position (AU) and velocity (m/s);
 * moons use orbitParent, orbitRadius (m) and orbitAngleDegrees and start on a circular orbit.
 */
public class BodyConfig {
    public String name;
    public double mass;
    public double radius;
    public float[] color;
    public float[] position;
    public float[] velocity;
    public boolean hasRings = false;
    public boolean isMoon = false;
    public String orbitParent;
    public double orbitRadius;
    public float orbitAngleDegrees = 0f;

    public BodyConfig copy() {
        BodyConfig c = new BodyConfig();
        c.name = name;
        c.mass = mass;
        c.radius = radius;
        c.color = color != null ? color.clone() : null;
        c.position = position != null ? position.clone() : null;
        c.velocity = velocity != null ? velocity.clone() : null;
        c.hasRings = hasRings;
        c.isMoon = isMoon;
        c.orbitParent = orbitParent;
        c.orbitRadius = orbitRadius;
        c.orbitAngleDegrees = orbitAngleDegrees;
        return c;
    }

    public boolean orbitsParent() {
        return orbitParent != null && !orbitParent.isEmpty();
    }
}
