package io.kostenarov.SolarSystem.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import io.kostenarov.SolarSystem.Main;
import io.kostenarov.SolarSystem.SimulationSettings;
import io.kostenarov.SolarSystem.physics.CelestialBody;
import io.kostenarov.SolarSystem.physics.PhysicalConstants;
import io.kostenarov.SolarSystem.physics.PhysicsEngine;
import io.kostenarov.SolarSystem.render.Renderer;
import io.kostenarov.SolarSystem.render.StarField;
import io.kostenarov.SolarSystem.system.BodyConfig;
import io.kostenarov.SolarSystem.system.BodyInitializer;
import io.kostenarov.SolarSystem.ui.SimulationHud;
import io.kostenarov.SolarSystem.ui.UiSkin;

/** Simulates and draws one solar system. */
public class SimulationScreen extends ScreenAdapter implements SimulationHud.Listener {
    private static final int RING_COUNT = 30;

    // Simulated seconds between trail points. Physics advances in steps of at least this long,
    // each split into substeps no longer than the integration step setting.
    private static final double TRAIL_SAMPLE_INTERVAL = 60 * 30;
    private static final float MAX_FRAME_TIME = 0.25f; // avoids a burst of catch-up steps after a stall

    // Camera zoom is metres per screen pixel. Closer than 10 km per pixel, float precision in
    // positions far from the Sun starts to show as jitter.
    private static final float MIN_ZOOM = 1e4f;
    private static final float MAX_ZOOM = 1e12f;
    private static final float DEFAULT_VIEW_WIDTH = 5 * PhysicalConstants.AU;

    // Keyboard rates per real second; each mouse wheel notch zooms by 15%
    private static final float PAN_SPEED = 600f;
    private static final float ZOOM_RATE = (float) Math.pow(1.01, 60);
    private static final float WHEEL_ZOOM_STEP = 1.15f;
    // A press that moves less than this many pixels is a click (select), not a drag (pan)
    private static final int CLICK_TOLERANCE = 4;
    // Clicks this close to a body's edge, in pixels, still select it
    private static final float PICK_MARGIN = 8f;

    private final Main game;
    private final SimulationSettings settings;
    private final Color[] ringColors = new Color[RING_COUNT];

    private final OrthographicCamera camera;
    private final Renderer renderer = new Renderer();
    private final StarField starField = new StarField();
    private final ShapeRenderer ringRenderer = new ShapeRenderer();
    private final SimulationHud hud;
    private final InputMultiplexer input;

    private final Array<CelestialBody> bodies;
    private final PhysicsEngine engine;
    private double physicsAccumulator = 0;
    private CelestialBody followTarget;
    private CelestialBody selected;
    private double simulatedSeconds = 0;
    private CelestialBody resetPoint;

    // Euler copy of the system, simulated alongside the main one only in "Both" mode
    private SimulationHud.IntegratorMode integratorMode;
    private Array<CelestialBody> comparisonBodies;
    private PhysicsEngine comparisonEngine;

    private final Vector3 tmp = new Vector3();

    public SimulationScreen(Main game, BodyConfig[] configs, String name) {
        this.game = game;
        this.settings = game.getSettings();

        bodies = new BodyInitializer().initializeFromConfigs(configs);
        engine = new PhysicsEngine(bodies, PhysicsEngine.Integrator.VELOCITY_VERLET, settings.integrationStep);

        // Reset (R) centres on the heaviest body, normally the star
        for (CelestialBody body : bodies) {
            if (resetPoint == null || body.getMass() > resetPoint.getMass()) resetPoint = body;
        }

        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hud = new SimulationHud(game.getSkin(), name, bodies, settings, this);
        applyIntegratorMode(hud.getIntegratorMode());
        resetToSun();

        input = new InputMultiplexer(hud.getStage(), new CameraInput());

        for (int i = 0; i < RING_COUNT; i++) {
            ringColors[i] = getRandomRingColor(i);
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(input);
    }

    private void select(CelestialBody body) {
        selected = body;
        hud.showInfo(body);
    }

    /** Selects the body with this name, if there is one. */
    public void selectByName(String name) {
        for (CelestialBody b : bodies) {
            if (b.getName().equals(name)) {
                select(b);
                return;
            }
        }
    }

    @Override
    public void onBodyPicked(CelestialBody body) {
        select(body);
        followTarget = body;
    }

    @Override
    public void onFollowToggled(CelestialBody body) {
        followTarget = followTarget == body ? null : body;
    }

    @Override
    public boolean isFollowing(CelestialBody body) {
        return followTarget == body;
    }

    @Override
    public void onSelectionCleared() {
        select(null);
    }

    @Override
    public void onSettingsClosed() {
        game.saveSettings();
    }

    @Override
    public void onMenuRequested() {
        game.showMenu();
    }

    /**
     * Mouse controls: click a body to select it, drag to pan, wheel to zoom towards the cursor.
     * The UI gets first refusal.
     */
    private class CameraInput extends InputAdapter {
        private boolean pressed;
        private boolean dragging;
        private int downX, downY, lastX, lastY;

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            if (hud.isUiInFocus(screenX, screenY)) return false;
            pressed = true;
            dragging = false;
            downX = lastX = screenX;
            downY = lastY = screenY;
            return true;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            if (!pressed) return false;
            if (!dragging && Math.abs(screenX - downX) <= CLICK_TOLERANCE && Math.abs(screenY - downY) <= CLICK_TOLERANCE) {
                return true;
            }
            dragging = true;
            // Dragging the view stops following a body
            followTarget = null;
            camera.translate(-(screenX - lastX) * camera.zoom, (screenY - lastY) * camera.zoom);
            lastX = screenX;
            lastY = screenY;
            return true;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            if (!pressed) return false;
            pressed = false;
            if (!dragging && button == Input.Buttons.LEFT) {
                // Clicking empty space closes the info panel
                select(bodyAt(screenX, screenY));
            }
            return true;
        }

        @Override
        public boolean scrolled(float amountX, float amountY) {
            int x = Gdx.input.getX();
            int y = Gdx.input.getY();
            if (hud.isUiInFocus(x, y)) return false;
            zoomAt(x, y, (float) Math.pow(WHEEL_ZOOM_STEP, amountY));
            return true;
        }
    }

    // Zooms keeping the world point under the cursor in place (or the followed body centred)
    private void zoomAt(int screenX, int screenY, float factor) {
        camera.update();
        camera.unproject(tmp.set(screenX, screenY, 0));
        float beforeX = tmp.x;
        float beforeY = tmp.y;

        camera.zoom = MathUtils.clamp(camera.zoom * factor, MIN_ZOOM, MAX_ZOOM);
        camera.update();

        if (followTarget == null) {
            camera.unproject(tmp.set(screenX, screenY, 0));
            camera.translate(beforeX - tmp.x, beforeY - tmp.y);
        }
    }

    private void handleKeys(float delta) {
        if (hud.isTyping()) return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            if (followTarget != null) followTarget = null;
            else followTarget = selected != null ? selected : bodyNearestScreenCentre();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) hud.togglePause();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) select(null);

        boolean uiInFocus = hud.isUiInFocus(Gdx.input.getX(), Gdx.input.getY());

        if (followTarget == null && !uiInFocus) {
            float pan = PAN_SPEED * camera.zoom * delta;
            if (Gdx.input.isKeyPressed(Input.Keys.A)) camera.translate(-pan, 0);
            if (Gdx.input.isKeyPressed(Input.Keys.D)) camera.translate(pan, 0);
            if (Gdx.input.isKeyPressed(Input.Keys.W)) camera.translate(0, pan);
            if (Gdx.input.isKeyPressed(Input.Keys.S)) camera.translate(0, -pan);
        }

        if (!hud.isOverScrollableArea(Gdx.input.getX(), Gdx.input.getY())) {
            float zoomFactor = (float) Math.pow(ZOOM_RATE, delta);
            if (Gdx.input.isKeyPressed(Input.Keys.Q)) camera.zoom *= zoomFactor;
            if (Gdx.input.isKeyPressed(Input.Keys.E)) camera.zoom /= zoomFactor;
            camera.zoom = MathUtils.clamp(camera.zoom, MIN_ZOOM, MAX_ZOOM);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            resetToSun();
        }
    }

    /** The body drawn under (or within a few pixels of) a screen point, or null. */
    private CelestialBody bodyAt(int screenX, int screenY) {
        camera.update();
        camera.unproject(tmp.set(screenX, screenY, 0));
        CelestialBody nearest = null;
        float nearestPixels = Float.MAX_VALUE;
        for (CelestialBody b : bodies) {
            float pixels = b.getPosition().dst(tmp.x, tmp.y) / camera.zoom;
            float reach = Renderer.drawRadius(b, settings, camera.zoom) / camera.zoom + PICK_MARGIN;
            if (pixels <= reach && pixels < nearestPixels) {
                nearestPixels = pixels;
                nearest = b;
            }
        }
        return nearest;
    }

    private CelestialBody bodyNearestScreenCentre() {
        CelestialBody nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (CelestialBody b : bodies) {
            float d = b.getPosition().dst2(camera.position.x, camera.position.y);
            if (d < nearestDistance) {
                nearestDistance = d;
                nearest = b;
            }
        }
        return nearest;
    }

    private void applyIntegratorMode(SimulationHud.IntegratorMode mode) {
        integratorMode = mode;
        engine.setIntegrator(mode == SimulationHud.IntegratorMode.EULER
            ? PhysicsEngine.Integrator.SEMI_IMPLICIT_EULER
            : PhysicsEngine.Integrator.VELOCITY_VERLET);

        if (mode == SimulationHud.IntegratorMode.BOTH) {
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
        camera.zoom = MathUtils.clamp(DEFAULT_VIEW_WIDTH / Gdx.graphics.getWidth(), MIN_ZOOM, MAX_ZOOM);
        camera.update();
    }

    @Override
    public void render(float frameDelta) {
        float delta = Math.min(frameDelta, MAX_FRAME_TIME);
        handleKeys(delta);

        if (hud.getIntegratorMode() != integratorMode) {
            applyIntegratorMode(hud.getIntegratorMode());
        }

        // Fixed-step physics: simulated time advances with real time, not with frame count
        if (!hud.isPaused()) {
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
                simulatedSeconds += step;
            }
        }

        if (!hud.isDisplayingMoonTrails()) {
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

        if (hud.isDisplayingStars()) {
            starField.render(camera);
        }
        // Comparison run first, so the main simulation is drawn on top of it
        if (comparisonBodies != null) {
            renderer.render(camera, comparisonBodies, settings, true);
        }
        renderer.render(camera, bodies, settings);
        renderRings();
        renderSelection();

        hud.render(frameDelta, simulatedSeconds);
    }

    private void renderRings() {
        ringRenderer.setProjectionMatrix(camera.combined);
        ringRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (CelestialBody b : bodies) {
            if (!b.hasRings()) continue;

            Vector2 pos = b.getPosition();
            float visualRadius = Renderer.drawRadius(b, settings, camera.zoom);
            // Roughly where Saturn's main rings sit, in planet radii
            float inner = visualRadius * 1.2f;
            float outer = visualRadius * 2.3f;

            for (int s = 0; s < RING_COUNT; s++) {
                float t = s / (float) (RING_COUNT - 1);
                ringRenderer.setColor(ringColors[s]);
                ringRenderer.circle(pos.x, pos.y, inner + (outer - inner) * t, 128);
            }
        }
        ringRenderer.end();
    }

    // Two accent-coloured rings around the selected body
    private void renderSelection() {
        if (selected == null) return;
        Vector2 p = selected.getPosition();
        float r = Renderer.drawRadius(selected, settings, camera.zoom);
        ringRenderer.setProjectionMatrix(camera.combined);
        ringRenderer.begin(ShapeRenderer.ShapeType.Line);
        ringRenderer.setColor(UiSkin.ACCENT);
        ringRenderer.circle(p.x, p.y, r + 5 * camera.zoom, 48);
        ringRenderer.circle(p.x, p.y, r + 6 * camera.zoom, 48);
        ringRenderer.end();
    }

    private void addTrailPoints(Array<CelestialBody> system) {
        for (CelestialBody b : system) {
            if (!b.isMoon || hud.isDisplayingMoonTrails()) {
                b.addTrailPoint(b.getPosition(), settings.trailLength);
            }
        }
    }

    private static void clearMoonTrails(Array<CelestialBody> system) {
        for (CelestialBody b : system) {
            if (b.isMoon) b.getTrail().clear();
        }
    }

    // Faint pastel rings whose opacity grows towards the outer edge
    private static Color getRandomRingColor(int ringNumber) {
        float alpha = (float) ringNumber / RING_COUNT;
        float chance = MathUtils.random();
        if (chance < 0.25f) return new Color(1f, 1f, 0.8f, alpha);   // soft yellowish
        if (chance < 0.50f) return new Color(0.8f, 0.9f, 1f, alpha);  // soft bluish
        if (chance < 0.75f) return new Color(0.9f, 0.9f, 1f, alpha);
        return new Color(1f, 1f, 1f, alpha);
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        hud.resize(width, height);
        camera.viewportWidth = width;
        camera.viewportHeight = height;
    }

    @Override
    public void dispose() {
        renderer.dispose();
        starField.dispose();
        ringRenderer.dispose();
        hud.dispose();
    }
}
