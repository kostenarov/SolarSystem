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


public class SolarSystemVisualiser extends ApplicationAdapter implements UIInitializer.BodySelectionListener {
    private OrthographicCamera camera;
    private Renderer renderer;
    private PhysicsEngine engine;
    private Array<CelestialBody> bodies;
    private CelestialBody followTarget;

    private ShapeRenderer starRenderer;
    private Array<Vector2> stars;

    private static final long STAR_COUNT = 250000;
    private static final float STAR_FIELD_RADIUS_MULTIPLIER = 1000000;
    private static final float VISUAL_RADIUS_SCALE = 400f;
    private static final float TIME_STEP = 60 * 600;
    private static final int TRAIL_MAX_POINTS = 1000;

    private UIInitializer uiInitializer;
    private BodyInitializer bodyInitializer;
    private float AU;

    @Override
    public void create() {
        bodyInitializer = new BodyInitializer();
        bodies = bodyInitializer.initializeBodies();
        AU = bodyInitializer.getAU(); // Get AU constant

        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0, 0, 0);
        camera.zoom = 5 * AU / Gdx.graphics.getWidth();
        camera.update();

        renderer = new Renderer();
        starRenderer = new ShapeRenderer();
        generateStars(AU);

        uiInitializer = new UIInitializer(bodies, this);
        Gdx.input.setInputProcessor(uiInitializer.getStage());

        engine = new PhysicsEngine(bodies);
    }

    @Override
    public void onBodySelected(CelestialBody body) {
        followTarget = body;
    }

    private void generateStars(float AU) {
        stars = new Array<>((int) STAR_COUNT);
        float range = STAR_FIELD_RADIUS_MULTIPLIER * AU;
        for (int i = 0; i < STAR_COUNT; i++) {
            float x = MathUtils.random(-range, range);
            float y = MathUtils.random(-range, range);
            stars.add(new Vector2(x, y));
        }
    }

    private void handleInput() {
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
            if (Gdx.input.isKeyPressed(Input.Keys.A)) camera.translate(-10 * camera.zoom, 0);
            if (Gdx.input.isKeyPressed(Input.Keys.D)) camera.translate(10 * camera.zoom, 0);
            if (Gdx.input.isKeyPressed(Input.Keys.W)) camera.translate(0, 10 * camera.zoom);
            if (Gdx.input.isKeyPressed(Input.Keys.S)) camera.translate(0, -10 * camera.zoom);
        }

        if (!uiInitializer.isOverScrollableArea(Gdx.input.getX(), Gdx.input.getY())) {
            if (Gdx.input.isKeyPressed(Input.Keys.Q)) camera.zoom *= 1.01f;
            if (Gdx.input.isKeyPressed(Input.Keys.E)) camera.zoom *= 0.99f;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.R)) {
            camera.position.set(0, 0, 0);
            camera.zoom = 5 * AU / Gdx.graphics.getWidth();
        }
    }

    @Override
    public void render() {
        handleInput();

        if (followTarget != null) {
            Vector2 p = followTarget.getPosition();
            camera.position.set(p.x, p.y, 0);
        }

        camera.update();

        if(!uiInitializer.isPaused())
            engine.update(TIME_STEP);

        for (CelestialBody b : bodies) {
            b.addTrailPoint(b.getPosition(), TRAIL_MAX_POINTS);
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        starRenderer.setProjectionMatrix(camera.combined);
        starRenderer.begin(ShapeRenderer.ShapeType.Point);
        starRenderer.setColor(Color.WHITE);
        for (Vector2 s : stars) {
            starRenderer.point(s.x, s.y, 0);
        }
        starRenderer.end();

        renderer.render(camera, bodies, VISUAL_RADIUS_SCALE);

        uiInitializer.getStage().act(Gdx.graphics.getDeltaTime());
        uiInitializer.getStage().draw();
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

        uiInitializer.dispose();
    }
}
