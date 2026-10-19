package io.kostenarov.SolarSystem.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.FloatArray;
import io.kostenarov.SolarSystem.physics.CelestialBody;
import io.kostenarov.SolarSystem.physics.PhysicalConstants;
import io.kostenarov.SolarSystem.physics.PhysicsEngine;
import io.kostenarov.SolarSystem.system.BodyConfig;
import io.kostenarov.SolarSystem.system.BodyInitializer;

/**
 * A map of the system being edited: where each body starts, and the paths the stars and
 * planets will follow, predicted by a quick simulation whenever the system changes.
 * Moons are drawn on enlarged orbits around their planet, since at true scale they would
 * sit on top of it.
 */
public class SystemPreview extends Widget implements Disposable {
    private static final int MAX_STEPS = 20000;
    private static final int POINTS_PER_PATH = 600;
    private static final int STEPS_PER_SHORTEST_ORBIT = 60;
    private static final float PADDING = 18f;
    private static final Color BACKGROUND = new Color(0.03f, 0.03f, 0.05f, 1f);

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font;

    private BodyConfig[] configs = new BodyConfig[0];
    private String selectedName;
    private long signature = Long.MIN_VALUE;

    // Per star/planet, in the same order as planetConfigs: initial position and predicted path (AU),
    // drawn relative to the heaviest body so a drifting star doesn't smear every path
    private final Array<BodyConfig> planetConfigs = new Array<>();
    private final Array<FloatArray> paths = new Array<>();
    private BodyConfig primary;
    private double predictedYears;
    private String problem;

    // AU -> local pixels
    private float scale = 1f, centreX, centreY;
    private float minX, maxX, minY, maxY;

    public SystemPreview(BitmapFont font) {
        this.font = font;
    }

    /** Called every frame with the current system; only re-runs the prediction when it changed. */
    public void setSystem(BodyConfig[] configs, String selectedName) {
        this.configs = configs;
        this.selectedName = selectedName;
        long newSignature = signature(configs);
        if (newSignature != signature) {
            signature = newSignature;
            predict();
        }
    }

    /** Name of the body drawn within a few pixels of the given local point, or null. */
    public String bodyAt(float x, float y) {
        String nearest = null;
        float nearestDistance = 12f * 12f;
        Vector2 p = new Vector2();
        for (BodyConfig c : configs) {
            if (!screenPosition(c, p)) continue;
            float d = p.dst2(x, y);
            if (d < nearestDistance) {
                nearestDistance = d;
                nearest = c.name;
            }
        }
        return nearest;
    }

    @Override
    public float getPrefWidth() {
        return 380;
    }

    @Override
    public float getPrefHeight() {
        return 372;
    }

    // ---- Prediction ----

    private void predict() {
        planetConfigs.clear();
        paths.clear();
        primary = null;
        problem = null;
        predictedYears = 0;

        for (BodyConfig c : configs) {
            if (c.orbitsParent()) continue;
            planetConfigs.add(c);
            paths.add(new FloatArray());
            if (primary == null || c.mass > primary.mass) primary = c;
        }

        if (planetConfigs.size >= 2) {
            for (BodyConfig c : planetConfigs) {
                if (!(c.mass > 0)) {
                    problem = "Every body needs a mass above 0 to predict orbits";
                    break;
                }
            }
            if (problem == null) simulate();
        }
        updateBounds();
    }

    private void simulate() {
        // Pick the step from the fastest orbit and the duration from the slowest (Kepler's third law)
        double shortest = Double.MAX_VALUE, longest = 0;
        for (BodyConfig c : planetConfigs) {
            if (c == primary) continue;
            double r = distanceMetres(c, primary);
            if (r == 0) continue;
            double period = 2 * Math.PI * Math.sqrt(r * r * r / (PhysicalConstants.G * (primary.mass + c.mass)));
            shortest = Math.min(shortest, period);
            longest = Math.max(longest, period);
        }
        if (longest == 0) return;

        double dt = shortest / STEPS_PER_SHORTEST_ORBIT;
        int steps = (int) Math.min(MAX_STEPS, Math.ceil(longest / dt));
        int sampleEvery = Math.max(1, steps / POINTS_PER_PATH);
        predictedYears = steps * dt / (365.25 * 86400);

        BodyConfig[] planets = planetConfigs.toArray(BodyConfig.class);
        Array<CelestialBody> bodies = new BodyInitializer().initializeFromConfigs(planets);
        PhysicsEngine engine = new PhysicsEngine(bodies, PhysicsEngine.Integrator.VELOCITY_VERLET, dt);
        int primaryIndex = planetConfigs.indexOf(primary, true);

        for (int step = 0; step <= steps; step++) {
            if (step > 0) engine.update(dt);
            if (step % sampleEvery != 0 && step != steps) continue;

            Vector2 star = bodies.get(primaryIndex).getPosition();
            for (int i = 0; i < bodies.size; i++) {
                Vector2 p = bodies.get(i).getPosition();
                paths.get(i).add(primary.position[0] + (p.x - star.x) / PhysicalConstants.AU);
                paths.get(i).add(primary.position[1] + (p.y - star.y) / PhysicalConstants.AU);
            }
        }
    }

    private static double distanceMetres(BodyConfig a, BodyConfig b) {
        double dx = (a.position[0] - b.position[0]) * (double) PhysicalConstants.AU;
        double dy = (a.position[1] - b.position[1]) * (double) PhysicalConstants.AU;
        return Math.hypot(dx, dy);
    }

    // Frame the starting positions; paths may extend the view, but only up to 3x the starting
    // extent so one body flung out of the system doesn't shrink everything else to a dot
    private void updateBounds() {
        if (planetConfigs.size == 0) {
            minX = minY = -1;
            maxX = maxY = 1;
            return;
        }
        float px = primary.position[0], py = primary.position[1];
        float extent = 0.5f;
        for (BodyConfig c : planetConfigs) {
            extent = Math.max(extent, Math.max(Math.abs(c.position[0] - px), Math.abs(c.position[1] - py)));
        }
        float limit = extent * 3f;
        minX = px - extent;
        maxX = px + extent;
        minY = py - extent;
        maxY = py + extent;
        for (FloatArray path : paths) {
            for (int i = 0; i < path.size; i += 2) {
                minX = Math.min(minX, Math.max(px - limit, path.get(i)));
                maxX = Math.max(maxX, Math.min(px + limit, path.get(i)));
                minY = Math.min(minY, Math.max(py - limit, path.get(i + 1)));
                maxY = Math.max(maxY, Math.min(py + limit, path.get(i + 1)));
            }
        }
    }

    private static long signature(BodyConfig[] configs) {
        long h = configs.length;
        for (BodyConfig c : configs) {
            h = h * 31 + String.valueOf(c.name).hashCode();
            h = h * 31 + Double.doubleToLongBits(c.mass);
            h = h * 31 + Double.doubleToLongBits(c.radius);
            h = h * 31 + String.valueOf(c.orbitParent).hashCode();
            h = h * 31 + Double.doubleToLongBits(c.orbitRadius);
            h = h * 31 + Float.floatToIntBits(c.orbitAngleDegrees);
            h = h * 31 + (c.hasRings ? 1 : 0);
            if (c.position != null) for (float v : c.position) h = h * 31 + Float.floatToIntBits(v);
            if (c.velocity != null) for (float v : c.velocity) h = h * 31 + Float.floatToIntBits(v);
            if (c.color != null) for (float v : c.color) h = h * 31 + Float.floatToIntBits(v);
        }
        return h;
    }

    // ---- Drawing ----

    private void updateTransform() {
        float w = getWidth() - 2 * PADDING, h = getHeight() - 2 * PADDING;
        scale = Math.min(w / Math.max(1e-6f, maxX - minX), h / Math.max(1e-6f, maxY - minY));
        centreX = getWidth() / 2 - (minX + maxX) / 2 * scale;
        centreY = getHeight() / 2 - (minY + maxY) / 2 * scale;
    }

    /** Local pixel position of a body, with moons placed on their enlarged display orbit. */
    private boolean screenPosition(BodyConfig c, Vector2 out) {
        if (!c.orbitsParent()) {
            if (c.position == null) return false;
            out.set(centreX + c.position[0] * scale, centreY + c.position[1] * scale);
            return true;
        }
        BodyConfig parent = find(c.orbitParent);
        if (parent == null || parent.orbitsParent() || !screenPosition(parent, out)) return false;
        float r = moonDisplayRadius(c, parent);
        float angle = (float) Math.toRadians(c.orbitAngleDegrees);
        out.add(r * (float) Math.cos(angle), r * (float) Math.sin(angle));
        return true;
    }

    // Moons of a planet get display orbits 6 px apart, in order of their real orbit radius
    private float moonDisplayRadius(BodyConfig moon, BodyConfig parent) {
        int rank = 0;
        for (BodyConfig c : configs) {
            if (c != moon && parent.name.equals(c.orbitParent) && c.orbitRadius < moon.orbitRadius) rank++;
        }
        return 12f + 6f * rank;
    }

    private BodyConfig find(String name) {
        for (BodyConfig c : configs) {
            if (name.equals(c.name)) return c;
        }
        return null;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        validate();
        updateTransform();
        float x0 = getX(), y0 = getY();

        batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(batch.getProjectionMatrix());
        shapes.setTransformMatrix(batch.getTransformMatrix());
        boolean clipped = clipBegin();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(BACKGROUND);
        shapes.rect(x0, y0, getWidth(), getHeight());
        shapes.end();

        Vector2 p = new Vector2();
        Vector2 q = new Vector2();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        // Predicted paths
        for (int b = 0; b < paths.size; b++) {
            FloatArray path = paths.get(b);
            Color c = colour(planetConfigs.get(b));
            shapes.setColor(c.r, c.g, c.b, 0.55f);
            for (int i = 2; i < path.size; i += 2) {
                shapes.line(x0 + centreX + path.get(i - 2) * scale, y0 + centreY + path.get(i - 1) * scale,
                    x0 + centreX + path.get(i) * scale, y0 + centreY + path.get(i + 1) * scale);
            }
        }
        // Enlarged moon orbits
        shapes.setColor(0.5f, 0.5f, 0.5f, 0.5f);
        for (BodyConfig c : configs) {
            if (!c.orbitsParent()) continue;
            BodyConfig parent = find(c.orbitParent);
            if (parent != null && !parent.orbitsParent() && screenPosition(parent, p)) {
                shapes.circle(x0 + p.x, y0 + p.y, moonDisplayRadius(c, parent), 32);
            }
        }
        // Selection ring and, for a planet, its starting velocity relative to the star.
        // The arrow is 40 px long at the speed of a circular orbit, so an orbit that will
        // be circular shows as a 40 px arrow at right angles to the line to the star.
        BodyConfig selected = selectedName != null ? find(selectedName) : null;
        if (selected != null && screenPosition(selected, p)) {
            shapes.setColor(Color.WHITE);
            shapes.circle(x0 + p.x, y0 + p.y, 9, 24);
            if (!selected.orbitsParent() && primary != null && selected != primary && selected.velocity != null) {
                double r = distanceMetres(selected, primary);
                double vx = selected.velocity[0] - primary.velocity[0];
                double vy = selected.velocity[1] - primary.velocity[1];
                if (r > 0) {
                    double circular = Math.sqrt(PhysicalConstants.G * primary.mass / r);
                    float k = (float) (40 / circular);
                    q.set(p).add((float) vx * k, (float) vy * k);
                    shapes.setColor(1f, 1f, 1f, 0.8f);
                    shapes.line(x0 + p.x, y0 + p.y, x0 + q.x, y0 + q.y);
                }
            }
        }
        shapes.end();

        // Bodies, stars and planets on top of their moons' orbits
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (BodyConfig c : configs) {
            if (!screenPosition(c, p)) continue;
            shapes.setColor(colour(c));
            float size = c == primary ? 7 : c.orbitsParent() ? 2.5f : 4.5f;
            shapes.circle(x0 + p.x, y0 + p.y, size, 20);
        }
        // Scale bar: 1 AU, or 10 AU when the view is wide
        float au = scale >= 30 ? 1 : 10;
        shapes.setColor(0.7f, 0.7f, 0.7f, 1f);
        shapes.rect(x0 + 10, y0 + 10, au * scale, 2);
        shapes.end();

        if (clipped) clipEnd();
        batch.begin();

        font.setColor(0.7f, 0.7f, 0.7f, 1f);
        font.draw(batch, (int) au + " AU", x0 + 10, y0 + 28);
        String caption = problem != null ? problem
            : paths.size > 0 && predictedYears > 0 ? String.format("Predicted paths: next %.1f years", predictedYears)
            : "Add a planet to see its orbit";
        font.draw(batch, caption, x0 + 8, y0 + getHeight() - 8);
        font.draw(batch, "Moon orbits enlarged", x0 + getWidth() - 150, y0 + 22);
        font.setColor(Color.WHITE);
    }

    private static Color colour(BodyConfig c) {
        float[] a = c.color;
        if (a == null || a.length < 3) return Color.WHITE;
        return new Color(a[0], a[1], a[2], 1f);
    }

    @Override
    public void dispose() {
        shapes.dispose();
    }
}
