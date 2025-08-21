package io.kostenarov.SolarSystem;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/** First screen of the application. Displayed after the application is created. */
public class FirstScreen implements Screen {
    private Stage stage;
    private ShapeRenderer shapeRenderer;
    private Texture sunTexture;
    private final Planet sun = new Planet("Sun", 0, 139, 0);
    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        stage.addActor(sun);
        Gdx.input.setInputProcessor(stage);
        sunTexture = new Texture(Gdx.files.internal("textures/sun.png"));
        shapeRenderer = new ShapeRenderer();
    }

    @Override
    public void render(float delta) {
        stage.act(delta);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.draw();
        // Draw the sun as a filled circle
        drawPlanet(stage.getBatch(), sun,
                   Gdx.graphics.getWidth() / 2f - sun.getDiameter() / 2f,
                   Gdx.graphics.getHeight() / 2f - sun.getDiameter() / 2f);
        // Draw your screen here. "delta" is the time since last render in seconds.
    }

    private void drawPlanet(Batch batch, Planet planet, float planetX, float planetY) {
        Texture texture = new Texture(Gdx.files.internal("textures/sun.png")); // Replace with your planet texture
        batch.begin();
        batch.draw(texture, planetX, planetY, planet.getDiameter()/2, planet.getDiameter()/2);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        // If the window is minimized on a desktop (LWJGL3) platform, width and height are 0, which causes problems.
        // In that case, we don't resize anything, and wait for the window to be a normal size before updating.
        if(width <= 0 || height <= 0) return;

        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
        // Invoked when your application is paused.
    }

    @Override
    public void resume() {
        // Invoked when your application is resumed after pause.
    }

    @Override
    public void hide() {
        // This method is called when another screen replaces this one.
    }

    @Override
    public void dispose() {
        stage.dispose();
        shapeRenderer.dispose();
    }
}
