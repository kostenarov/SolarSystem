package io.kostenarov.SolarSystem.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.kostenarov.SolarSystem.physics.CelestialBody;
import io.kostenarov.SolarSystem.physics.PhysicalConstants;

/** Turns save-file entries into simulated bodies. */
public class BodyInitializer {

    /**
     * Stars and planets start where their config says; moons start on a circular orbit around
     * their parent at the given radius and angle. Moons whose parent is missing are skipped
     * (see {@link SystemFiles#validate}).
     */
    public Array<CelestialBody> initializeFromConfigs(BodyConfig[] configs) {
        Array<CelestialBody> bodies = new Array<>();

        for (BodyConfig c : configs) {
            if (c.orbitsParent()) continue;

            float au = PhysicalConstants.AU;
            Vector2 pos = c.position != null ? new Vector2(c.position[0] * au, c.position[1] * au) : new Vector2();
            Vector2 vel = c.velocity != null ? new Vector2(c.velocity[0], c.velocity[1]) : new Vector2();
            bodies.add(new CelestialBody(c.name, (float) c.mass, (float) c.radius, toColor(c.color), pos, vel, c.hasRings, false));
        }

        int planetCount = bodies.size;
        for (BodyConfig c : configs) {
            if (!c.orbitsParent()) continue;
            CelestialBody parent = null;
            for (int i = 0; i < planetCount; i++) {
                if (bodies.get(i).getName().equals(c.orbitParent)) {
                    parent = bodies.get(i);
                    break;
                }
            }
            if (parent == null) continue;
            bodies.add(createMoon(c, parent));
        }

        for (CelestialBody b : bodies) {
            b.addTrailPoint(b.getPosition(), 1);
        }
        return bodies;
    }

    private static CelestialBody createMoon(BodyConfig c, CelestialBody parent) {
        double r = c.orbitRadius;
        double angle = Math.toRadians(c.orbitAngleDegrees);

        // Circular orbit speed around the parent, on top of the parent's own velocity
        double speed = Math.sqrt(PhysicalConstants.G * ((double) parent.getMass() + c.mass) / r);
        Vector2 parentPos = parent.getPosition();
        Vector2 parentVel = parent.getVelocity();
        Vector2 pos = new Vector2((float) (parentPos.x + r * Math.cos(angle)), (float) (parentPos.y + r * Math.sin(angle)));
        Vector2 vel = new Vector2((float) (parentVel.x - speed * Math.sin(angle)), (float) (parentVel.y + speed * Math.cos(angle)));

        CelestialBody moon = new CelestialBody(c.name, (float) c.mass, (float) c.radius, toColor(c.color), pos, vel, c.hasRings, true);
        moon.orbitParent = parent;
        return moon;
    }

    private static Color toColor(float[] arr) {
        if (arr == null) return Color.WHITE;
        float r = arr.length > 0 ? arr[0] : 1f;
        float g = arr.length > 1 ? arr[1] : 1f;
        float b = arr.length > 2 ? arr[2] : 1f;
        float a = arr.length > 3 ? arr[3] : 1f;
        return new Color(r, g, b, a);
    }
}
