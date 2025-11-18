package io.kostenarov.SolarSystem.Classes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.badlogic.gdx.utils.Json;

public class BodyInitializer {

    private static final double G = 6.67430e-11;
    private final float AU = 1.496e11f;
    private static final int TRAIL_MAX_POINTS = 1000;

    public Array<CelestialBody> initializeBodies() {
        Array<CelestialBody> bodies = new Array<>();

        CelestialBody sun = initializeBody(bodies, "Sun", 1.989e30f, 100.9634e11f, Color.YELLOW, new Vector2(0, 0), new Vector2(0, 700));
        initializeBody(bodies, "Mercury", 3.3011e23f, 2.4397e6f, Color.FIREBRICK, new Vector2(0, 0.387f * AU), new Vector2(-47360, 0));
        initializeBody(bodies, "Venus", 4.8675e24f, 6.0518e6f, Color.CHARTREUSE, new Vector2(-0.723f * AU, 0), new Vector2(0, -35020));
        CelestialBody earth = initializeBody(bodies, "Earth", 5.972e24f, 6.371e6f, Color.BLUE, new Vector2(AU, 0), new Vector2(0, 29780));
        CelestialBody mars = initializeBody(bodies, "Mars", 6.39e23f, 3.389e6f, Color.ORANGE, new Vector2(0, -1.524f * AU), new Vector2(24070, 0));
        CelestialBody jupiterLike = initializeBody(bodies, "Jupiter-Like", 1.898e28f, 2.991e7f, Color.GOLDENROD, new Vector2(-3.5f * AU, 0), new Vector2(0, -18000));
        initializeBody(bodies, "Saturn-Like", 5.683e26f, 6.003e6f, Color.TEAL, new Vector2(9.5f * AU, 0), new Vector2(0, 9690), true);
        initializeBody(bodies, "Uranus-Like", 8.681e25f, 2.5362e7f, Color.OLIVE, new Vector2(0, -19.8f * AU), new Vector2(6800, 0));
        initializeBody(bodies, "Neptune-Like", 1.024e26f, 2.4622e7f, Color.CYAN, new Vector2(-30.0f * AU, 0), new Vector2(0, -5430));

        createMoon(bodies, earth, "Moon", 7.342e22f, 1.737e6f, 3.844e8f, (float) Math.toRadians(30), Color.CLEAR_WHITE);

        createMoon(bodies, mars, "Phobos-Moon", 1.0659e16f, 1.1e4f, 9.378e6f, (float) Math.toRadians(-45), Color.LIGHT_GRAY);
        createMoon(bodies, mars, "Deimos-Moon", 1.4762e15f, 6.2e3f, 2.346e7f, (float) Math.toRadians(75), Color.DARK_GRAY);

        createMoon(bodies, jupiterLike, "Io-Like", 8.9319e22f, 1.8216e6f, 4.217e8f, (float) Math.toRadians(60), Color.ORANGE);
        createMoon(bodies, jupiterLike, "Europa-Like", 4.7998e22f, 1.5608e6f, 6.709e8f, (float) Math.toRadians(-90), Color.LIGHT_GRAY);
        createMoon(bodies, jupiterLike, "Ganymede-Like", 1.4819e23f, 2.6341e6f, 1.0704e9f, (float) Math.toRadians(120), Color.DARK_GRAY);
        createMoon(bodies, jupiterLike, "Titan-Moon", 1.3452e23f, 2.575e6f, 1.2219e9f, (float) Math.toRadians(0), Color.CHARTREUSE);
        createMoon(bodies, jupiterLike, "Callisto-Like", 1.0759e23f, 2.4103e6f, 1.8827e9f, (float) Math.toRadians(-150), Color.BLACK);
        createMoon(bodies, jupiterLike, "Dino-Moon", 5.0e20f, 5.0e5f, 2.5e9f, (float) Math.toRadians(45), Color.SALMON);
        createMoon(bodies, jupiterLike, "Rexo-Moon", 8.0e20f, 7.0e5f, 3.0e9f, (float) Math.toRadians(-30), Color.WHITE);
        createMoon(bodies, jupiterLike, "Zork-Moon", 1.2e21f, 9.0e5f, 3.5e9f, (float) Math.toRadians(90), Color.MAROON);
        createMoon(bodies, jupiterLike, "Blip-Moon", 6.0e20f, 6.0e5f, 4.0e9f, (float) Math.toRadians(-120), Color.SLATE);

        for (CelestialBody b : bodies) {
            b.addTrailPoint(b.getPosition(), TRAIL_MAX_POINTS);
        }

        return bodies;
    }

    private CelestialBody initializeBody(Array<CelestialBody> bodies, String name, double mass, double radius, Color color, Vector2 position, Vector2 velocity) {
        CelestialBody body = new CelestialBody(name, (float) mass, (float) radius, color, position, velocity);
        bodies.add(body);
        return body;
    }

    private CelestialBody initializeBody(Array<CelestialBody> bodies, String name, double mass, double radius, Color color, Vector2 position, Vector2 velocity, boolean hasRings) {
        CelestialBody body = new CelestialBody(name, (float) mass, (float) radius, color, position, velocity, hasRings, false);
        bodies.add(body);
        return body;
    }

    private void createMoon(Array<CelestialBody> bodies, CelestialBody parent, String name, double mass, double radius, double orbitRadius, float angleRadians, Color color) {
        float px = parent.getPosition().x + (float) (orbitRadius * Math.cos(angleRadians));
        float py = parent.getPosition().y + (float) (orbitRadius * Math.sin(angleRadians));

        double v = Math.sqrt(G * (double) parent.getMass() / orbitRadius);

        float vxRel = (float) (-v * Math.sin(angleRadians));
        float vyRel = (float) (v * Math.cos(angleRadians));

        Vector2 parentVel = parent.getVelocity();
        float vx = parentVel.x + vxRel;
        float vy = parentVel.y + vyRel;

        CelestialBody moon = new CelestialBody(
            name,
            (float) mass,
            (float) radius,
            color,
            new Vector2(px, py),
            new Vector2(vx, vy),
            false,
            true
        );

        bodies.add(moon);
    }

    public Array<CelestialBody> initializeFromJson(String jsonFilePath) throws IOException {
        String jsonText = Files.readString(Paths.get(jsonFilePath), StandardCharsets.UTF_8);
        Json json = new Json();
        BodyConfig[] configs = json.fromJson(BodyConfig[].class, jsonText);

        Array<CelestialBody> bodies = new Array<>();
        List<BodyConfig> moonConfigs = new ArrayList<>();

        for (BodyConfig c : configs) {
            if (c.orbitParent != null && !c.orbitParent.isEmpty()) {
                moonConfigs.add(c);
                continue;
            }

            Vector2 pos = c.position != null ? new Vector2(c.position[0] * AU, c.position[1] * AU) : new Vector2(0, 0);
            Vector2 vel = c.velocity != null ? new Vector2(c.velocity[0], c.velocity[1]) : new Vector2(0, 0);
            Color col = toColor(c.color);

            CelestialBody body = new CelestialBody(
                c.name,
                (float) c.mass,
                (float) c.radius,
                col,
                pos,
                vel,
                c.hasRings,
                c.isMoon
            );

            bodies.add(body);
        }

        for (BodyConfig c : moonConfigs) {
            CelestialBody parent = null;
            for (CelestialBody b : bodies) {
                if (b.getName().equals(c.orbitParent)) {
                    parent = b;
                    break;
                }
            }
            if (parent == null) continue;

            double orbitR = c.orbitRadius;
            float angleRad = (float) Math.toRadians(c.orbitAngleDegrees);

            float px = parent.getPosition().x + (float) (orbitR * Math.cos(angleRad));
            float py = parent.getPosition().y + (float) (orbitR * Math.sin(angleRad));

            double v = Math.sqrt(G * (double) parent.getMass() / orbitR);
            float vxRel = (float) (-v * Math.sin(angleRad));
            float vyRel = (float) (v * Math.cos(angleRad));

            Vector2 parentVel = parent.getVelocity();
            float vx = parentVel.x + vxRel;
            float vy = parentVel.y + vyRel;

            Color col = toColor(c.color);

            CelestialBody moon = new CelestialBody(
                c.name,
                (float) c.mass,
                (float) c.radius,
                col,
                new Vector2(px, py),
                new Vector2(vx, vy),
                c.hasRings,
                true
            );

            bodies.add(moon);
        }

        for (CelestialBody b : bodies) {
            b.addTrailPoint(b.getPosition(), TRAIL_MAX_POINTS);
        }

        return bodies;
    }

    private static Color toColor(float[] arr) {
        if (arr == null) return Color.WHITE;
        float r = arr.length > 0 ? arr[0] : 1f;
        float g = arr.length > 1 ? arr[1] : 1f;
        float b = arr.length > 2 ? arr[2] : 1f;
        float a = arr.length > 3 ? arr[3] : 1f;
        return new Color(r, g, b, a);
    }

    public float getAU() {
        return AU;
    }
}
