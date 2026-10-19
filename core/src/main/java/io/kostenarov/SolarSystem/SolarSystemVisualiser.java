package io.kostenarov.SolarSystem;

import io.kostenarov.SolarSystem.physics.PhysicalConstants;
import io.kostenarov.SolarSystem.system.BodyConfig;
import io.kostenarov.SolarSystem.system.BodyInitializer;
import io.kostenarov.SolarSystem.system.SystemFiles;
import io.kostenarov.SolarSystem.physics.CelestialBody;
import io.kostenarov.SolarSystem.physics.PhysicsEngine;
import io.kostenarov.SolarSystem.render.Renderer;
import io.kostenarov.SolarSystem.render.StarField;
import io.kostenarov.SolarSystem.ui.UIInitializer;
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

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.color;


public class SolarSystemVisualiser extends ApplicationAdapter implements UIInitializer.Listener {
    private int ringCount = 30;
    private float AU;

    private Color[] ringColors = new Color[ringCount];

    private OrthographicCamera camera;
    private Renderer renderer;
    private PhysicsEngine engine;
    private Array<CelestialBody> bodies;
    private CelestialBody followTarget;
    private CelestialBody resetPoint;

    private StarField starField;
    private ShapeRenderer ringRenderer; // added for rings

    // Simulated seconds between trail points. Physics advances in steps of at least this long,
    // each split into substeps no longer than the integration step setting.
    private static final double TRAIL_SAMPLE_INTERVAL = 60 * 30;
    private static final float MAX_FRAME_TIME = 0.25f; // avoids a burst of catch-up steps after a stall
    private static final String DEFAULT_SYSTEM = "exampleSystem";

    // Rates per real second (previously 10 units and 1% zoom per frame at 60 FPS)
    private static final float PAN_SPEED = 600f;
    private static final float ZOOM_RATE = (float) Math.pow(1.01, 60);

    private final SimulationSettings settings = new SimulationSettings();
    private double physicsAccumulator = 0;

    // The system being simulated, as loaded, so the editor can start from it
    private BodyConfig[] systemConfigs;
    private String systemName;

    // Euler copy of the system, simulated alongside the main one only in "Both" mode
    private UIInitializer.IntegratorMode integratorMode;
    private Array<CelestialBody> comparisonBodies;
    private PhysicsEngine comparisonEngine;

    private UIInitializer uiInitializer;
    private BodyInitializer bodyInitializer;

    @Override
    public void create() {
        settings.load(Gdx.app.getPreferences(SimulationSettings.PREFERENCES_NAME));
        bodyInitializer = new BodyInitializer();
        AU = PhysicalConstants.AU;
        BodyConfig[] initialSystem = SystemFiles.read(DEFAULT_SYSTEM);

        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0, 0, 0);
        camera.zoom = 5 * AU / Gdx.graphics.getWidth();
        camera.update();

        renderer = new Renderer();
        starField = new StarField();
        ringRenderer = new ShapeRenderer();

        uiInitializer = new UIInitializer(new Array<CelestialBody>(), settings, this);
        Gdx.input.setInputProcessor(uiInitializer.getStage());
        loadSystem(initialSystem, DEFAULT_SYSTEM);

        for(int i = 0; i < ringCount; i++) {
            ringColors[i] = getRandomRingColor(i);
        }

    }

    /** Replaces the simulated system and restarts the simulation from its initial state. */
    private void loadSystem(BodyConfig[] configs, String name) {
        systemConfigs = configs;
        systemName = name;
        bodies = bodyInitializer.initializeFromConfigs(configs);
        engine = new PhysicsEngine(bodies, PhysicsEngine.Integrator.VELOCITY_VERLET, settings.integrationStep);
        applyIntegratorMode(uiInitializer.getIntegratorMode());
        physicsAccumulator = 0;
        followTarget = null;

        // Reset (R) centres on the heaviest body, normally the star
        resetPoint = null;
        for (CelestialBody body : bodies) {
            if (resetPoint == null || body.getMass() > resetPoint.getMass()) resetPoint = body;
        }
        uiInitializer.updateBodyList(bodies);
    }

    @Override
    public void onBodySelected(CelestialBody body) {
        followTarget = body;
    }

    @Override
    public void onSystemApplied(BodyConfig[] configs, String name) {
        loadSystem(configs, name);
        resetToSun();
    }

    @Override
    public BodyConfig[] getCurrentSystem() {
        return systemConfigs;
    }

    @Override
    public String getCurrentSystemName() {
        return systemName;
    }

    @Override
    public void onSettingsClosed() {
        settings.save(Gdx.app.getPreferences(SimulationSettings.PREFERENCES_NAME));
    }

    private void handleInput(float delta) {
        if (uiInitializer.isTyping()) return;

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
            comparisonEngine = new PhysicsEngine(comparisonBodies, PhysicsEngine.Integrator.SEMI_IMPLICIT_EULER, settings.integrationStep);
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
            double step = Math.max(TRAIL_SAMPLE_INTERVAL, settings.integrationStep);
            engine.setMaxSubstep(settings.integrationStep);
            if (comparisonEngine != null) comparisonEngine.setMaxSubstep(settings.integrationStep);

            physicsAccumulator += delta * settings.timeScale;
            while (physicsAccumulator >= step) {
                engine.update(step);
                addTrailPoints(bodies);
                if (comparisonEngine != null) {
                    comparisonEngine.update(step);
                    addTrailPoints(comparisonBodies);
                }
                physicsAccumulator -= step;
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

        if (uiInitializer.isDisplayingStars()) {
            starField.render(camera);
        }
        // Comparison run first, so the main simulation is drawn on top of it
        if (comparisonBodies != null) {
            renderer.render(camera, comparisonBodies, settings, true);
        }
        renderer.render(camera, bodies, settings);

        ringRenderer.setProjectionMatrix(camera.combined);
        ringRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (CelestialBody b : bodies) {
            if (!b.hasRings()) continue;

            Vector2 pos = b.getPosition();
            float visualRadius = Renderer.drawRadius(b, settings, camera.zoom);

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
                b.addTrailPoint(b.getPosition(), settings.trailLength);
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
        settings.save(Gdx.app.getPreferences(SimulationSettings.PREFERENCES_NAME));
        renderer.dispose();
        if (starField != null) starField.dispose();
        if (ringRenderer != null) ringRenderer.dispose();

        uiInitializer.dispose();
    }
}
