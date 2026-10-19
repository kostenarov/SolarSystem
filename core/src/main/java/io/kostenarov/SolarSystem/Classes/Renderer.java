package io.kostenarov.SolarSystem.Classes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Renderer {
    private static final float GHOST_TRAIL_BRIGHTNESS = 0.5f;
    // Longer trails are drawn using every n-th point, so drawing cost stays bounded
    private static final int MAX_TRAIL_SEGMENTS = 4000;

    private final ShapeRenderer shape;

    // Radius in world units that a body is drawn with. With the camera viewport sized to the
    // window, one screen pixel covers camera.zoom world units.
    public static float drawRadius(CelestialBody body, SimulationSettings settings, float zoom) {
        return Math.max(settings.minBodyPixels * zoom, body.getRadius() * settings.bodySizeScale);
    }

    public Renderer() {
        shape = new ShapeRenderer();
        shape.setAutoShapeType(true);
    }

    public void render(OrthographicCamera camera, Array<CelestialBody> bodies, SimulationSettings settings) {
        render(camera, bodies, settings, false);
    }

    /**
     * @param ghost draw as a comparison run: dimmed trails and hollow bodies, so it can sit
     *              underneath the main simulation without hiding it
     */
    public void render(OrthographicCamera camera, Array<CelestialBody> bodies, SimulationSettings settings, boolean ghost) {
        shape.setProjectionMatrix(camera.combined);
        float brightness = ghost ? GHOST_TRAIL_BRIGHTNESS : 1f;

        // Blending makes the trails fade out towards their oldest end
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shape.begin(ShapeRenderer.ShapeType.Line);
        for (CelestialBody body : bodies) {
            Trail trail = body.getTrail();
            int last = trail.size() - 1;
            if (last < 1) continue;

            Color base = body.getColor();
            int stride = Math.max(1, (last + MAX_TRAIL_SEGMENTS - 1) / MAX_TRAIL_SEGMENTS);
            int i = 0;
            while (i < last) {
                int next = Math.min(i + stride, last); // always end on the newest point
                float t = (float) next / last; // 0 = oldest, 1 = newest
                float alpha = 0.05f + 0.95f * t;
                shape.setColor(base.r * brightness, base.g * brightness, base.b * brightness, alpha);
                shape.line(trail.getX(i), trail.getY(i), trail.getX(next), trail.getY(next));
                i = next;
            }
        }
        shape.end();

        shape.begin(ghost ? ShapeRenderer.ShapeType.Line : ShapeRenderer.ShapeType.Filled);
        for (CelestialBody body : bodies) {
            Color c = body.getColor();
            shape.setColor(c);
            Vector2 p = body.getPosition();
            shape.circle(p.x, p.y, drawRadius(body, settings, camera.zoom), 32);
        }
        shape.end();
    }

    public void dispose() {
        shape.dispose();
    }
}
