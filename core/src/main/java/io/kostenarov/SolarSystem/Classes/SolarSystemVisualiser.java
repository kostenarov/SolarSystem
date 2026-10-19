package io.kostenarov.SolarSystem.Classes;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.io.IOException;

import static com.badlogic.gdx.math.MathUtils.random;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.color;


public class SolarSystemVisualiser extends ApplicationAdapter implements UIInitializer.BodySelectionListener {
    private int ringCount = 30;
    private float AU;

    private Color[] ringColors = new Color[ringCount];

    private OrthographicCamera camera;
    private Renderer renderer;
    private PhysicsEngine engine;
    private Array<CelestialBody> bodies;
    private CelestialBody followTarget;
    private CelestialBody resetPoint;

    private ShapeRenderer starRenderer;
    private ShapeRenderer ringRenderer; // added for rings
    private Array<Vector2> stars;

    private static final long STAR_COUNT = 250000;
    private static final float STAR_FIELD_RADIUS_MULTIPLIER = 1000000;
    private static final float VISUAL_RADIUS_SCALE = 400f;
    private static final float PHYSICS_DT = 60 * 30; // 30 simulated minutes per physics step
    private static final float STEPS_PER_REAL_SECOND = 60f;
    private static final float SIM_SECONDS_PER_REAL_SECOND = PHYSICS_DT * STEPS_PER_REAL_SECOND;
    private static final float MAX_FRAME_TIME = 0.25f; // avoids a burst of catch-up steps after a stall
    private static final int TRAIL_MAX_POINTS = 100000;

    // Rates per real second (previously 10 units and 1% zoom per frame at 60 FPS)
    private static final float PAN_SPEED = 600f;
    private static final float ZOOM_RATE = (float) Math.pow(1.01, 60);

    // Each physics step is split into substeps of at most this many seconds, so that
    // fast inner moons (Phobos orbits in ~7.6 h) get enough steps per orbit to stay stable
    private static final double MAX_SUBSTEP = 5 * 60;

    private float physicsAccumulator = 0f;

    // Euler copy of the system, simulated alongside the main one only in "Both" mode
    private UIInitializer.IntegratorMode integratorMode;
    private Array<CelestialBody> comparisonBodies;
    private PhysicsEngine comparisonEngine;

    private UIInitializer uiInitializer;
    private BodyInitializer bodyInitializer;

    @Override
    public void create() {
        bodyInitializer = new BodyInitializer();
        try {
            bodies = bodyInitializer.initializeFromJson("saves/exampleSystem.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        AU = bodyInitializer.getAU(); // Get AU constant

        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0, 0, 0);
        camera.zoom = 5 * AU / Gdx.graphics.getWidth();
        camera.update();

        renderer = new Renderer();
        starRenderer = new ShapeRenderer();
        ringRenderer = new ShapeRenderer();
        generateStars(AU);

        uiInitializer = new UIInitializer(bodies, this);
        Gdx.input.setInputProcessor(uiInitializer.getStage());

        engine = new PhysicsEngine(bodies, PhysicsEngine.Integrator.VELOCITY_VERLET, MAX_SUBSTEP);
        applyIntegratorMode(uiInitializer.getIntegratorMode());
        for (CelestialBody body : bodies) {
            if ("Sun".equals(body.getName())) {
                resetPoint = body;
                break;
            }
        }

        for(int i = 0; i < ringCount; i++) {
            ringColors[i] = getRandomRingColor(i);
        }

    }

    @Override
    public void onBodySelected(CelestialBody body) {
        followTarget = body;
    }

    private void generateStars(float AU) {
        stars = new Array<>((int) STAR_COUNT);
        float range = STAR_FIELD_RADIUS_MULTIPLIER * AU;
        for (int i = 0; i < STAR_COUNT; i++) {
            float x = random(-range, range);
            float y = random(-range, range);
            stars.add(new Vector2(x, y));
        }
    }

    private void handleInput(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            if (followTarget == null && bodies.size > 0) {
                followTarget = bodies.first();
                for (CelestialBody b : bodies) {
                    if ("Earth".equals(b.getName())) {
                        followTarget = b;
                        break;
                    }
                }
            } else {
                followTarget = null;
            }
        }

        boolean uiInFocus = uiInitializer.isUiInFocus(Gdx.input.getX(), Gdx.input.getY());

        if (followTarget == null && !uiInFocus) {
            float pan = PAN_SPEED * camera.zoom * delta;
            if (Gdx.input.isKeyPressed(Input.Keys.A)) camera.translate(-pan, 0);
            if (Gdx.input.isKeyPressed(Input.Keys.D)) camera.translate(pan, 0);
            if (Gdx.input.isKeyPressed(Input.Keys.W)) camera.translate(0, pan);
            if (Gdx.input.isKeyPressed(Input.Keys.S)) camera.translate(0, -pan);
        }

        if (!uiInitializer.isOverScrollableArea(Gdx.input.getX(), Gdx.input.getY())) {
            float zoomFactor = (float) Math.pow(ZOOM_RATE, delta);
            if (Gdx.input.isKeyPressed(Input.Keys.Q)) camera.zoom *= zoomFactor;
            if (Gdx.input.isKeyPressed(Input.Keys.E)) camera.zoom /= zoomFactor;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            resetToSun();
        }
    }

    private void applyIntegratorMode(UIInitializer.IntegratorMode mode) {
        integratorMode = mode;
        engine.setIntegrator(mode == UIInitializer.IntegratorMode.EULER
            ? PhysicsEngine.Integrator.SEMI_IMPLICIT_EULER
            : PhysicsEngine.Integrator.VELOCITY_VERLET);

        if (mode == UIInitializer.IntegratorMode.BOTH) {
            // Start the Euler copy from the current state so both runs diverge from the same point
            comparisonBodies = new Array<>(bodies.size);
            for (CelestialBody b : bodies) {
                comparisonBodies.add(new CelestialBody(b));
            }
            comparisonEngine = new PhysicsEngine(comparisonBodies, PhysicsEngine.Integrator.SEMI_IMPLICIT_EULER, MAX_SUBSTEP);
        } else {
            comparisonBodies = null;
            comparisonEngine = null;
        }
    }

    public void resetToSun() {
        if (resetPoint != null) {
            Vector2 p = resetPoint.getPosition();
            camera.position.set(p.x, p.y, 0);
        } else {
            camera.position.set(0, 0, 0);
        }
        followTarget = null;
        camera.zoom = 5 * AU / Gdx.graphics.getWidth();
        camera.update();
    }

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), MAX_FRAME_TIME);
        handleInput(delta);

        if (uiInitializer.getIntegratorMode() != integratorMode) {
            applyIntegratorMode(uiInitializer.getIntegratorMode());
        }

        // Fixed-step physics: simulated time advances with real time, not with frame count
        if (!uiInitializer.isPaused()) {
            physicsAccumulator += delta * SIM_SECONDS_PER_REAL_SECOND;
            while (physicsAccumulator >= PHYSICS_DT) {
                engine.update(PHYSICS_DT);
                addTrailPoints(bodies);
                if (comparisonEngine != null) {
                    comparisonEngine.update(PHYSICS_DT);
                    addTrailPoints(comparisonBodies);
                }
                physicsAccumulator -= PHYSICS_DT;
            }
        }

        if (!uiInitializer.isDisplayingMoonTrails()) {
            clearMoonTrails(bodies);
            if (comparisonBodies != null) clearMoonTrails(comparisonBodies);
        }

        if (followTarget != null) {
            Vector2 p = followTarget.getPosition();
            camera.position.set(p.x, p.y, 0);
        }

        camera.update();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        starRenderer.setProjectionMatrix(camera.combined);

        if(uiInitializer.isDisplayingStars()) {
            starRenderer.begin(ShapeRenderer.ShapeType.Point);
            starRenderer.setColor(Color.WHITE);
            for (Vector2 s : stars) {
                starRenderer.point(s.x, s.y, 0);
            }
            starRenderer.end();
        }
        // Comparison run first, so the main simulation is drawn on top of it
        if (comparisonBodies != null) {
            renderer.render(camera, comparisonBodies, VISUAL_RADIUS_SCALE, true);
        }
        renderer.render(camera, bodies, VISUAL_RADIUS_SCALE);

        ringRenderer.setProjectionMatrix(camera.combined);
        ringRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (CelestialBody b : bodies) {
            if (!b.hasRings()) continue;

            Vector2 pos = b.getPosition();
            float visualRadius = Renderer.drawRadius(b, VISUAL_RADIUS_SCALE, camera.zoom);

            float inner = visualRadius * 1.5f;
            float outer = visualRadius * 2.5f;
            int segments = 128;

            for (int s = 0; s < ringCount; s++) {
                float t = s / (float) Math.max(1, ringCount - 1);
                float r = inner + (outer - inner) * t;
                ringRenderer.setColor(ringColors[s]);
                ringRenderer.circle(pos.x, pos.y, r, segments);
            }
        }
        ringRenderer.end();

        uiInitializer.getStage().act(Gdx.graphics.getDeltaTime());
        uiInitializer.getStage().draw();
    }

    private void addTrailPoints(Array<CelestialBody> system) {
        for (CelestialBody b : system) {
            if (!b.isMoon || uiInitializer.isDisplayingMoonTrails()) {
                b.addTrailPoint(b.getPosition(), TRAIL_MAX_POINTS);
            }
        }
    }

    private static void clearMoonTrails(Array<CelestialBody> system) {
        for (CelestialBody b : system) {
            if (b.isMoon) b.getTrail().clear();
        }
    }

    private Color getRandomRingColor(int ringNumber) {
        float chance = MathUtils.random();
        Color color = new Color();
        if (chance < 0.25f) {
            return new Color(1f, 1f, 0.8f, (float) ringNumber /ringCount); // Soft yellowish
        } else if (chance < 0.50f) {
            return new Color(0.8f, 0.9f, 1f, (float) ringNumber /ringCount); // Soft bluish
        } else if (chance < 0.75f) {
            return new Color(0.9f, 0.9f, 1f, (float) ringNumber /ringCount);
        } else {
            return new Color(1f, 1f, 1f, (float) ringNumber /ringCount);
        }
    }

    @Override
    public void resize(int width, int height) {
        uiInitializer.resize(width, height);

        camera.viewportWidth = width;
        camera.viewportHeight = height;
    }

    @Override
    public void dispose() {
        renderer.dispose();
        if (starRenderer != null) starRenderer.dispose();
        if (ringRenderer != null) ringRenderer.dispose();

        uiInitializer.dispose();
    }
}
