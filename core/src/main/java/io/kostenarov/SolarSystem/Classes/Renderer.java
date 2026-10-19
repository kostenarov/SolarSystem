package io.kostenarov.SolarSystem.Classes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Renderer {
    // Bodies are never drawn smaller than this many screen pixels, whatever the zoom
    private static final float MIN_BODY_PIXELS = 3f;

    private final ShapeRenderer shape;

    // Radius in world units that a body is drawn with. With the camera viewport sized to the
    // window, one screen pixel covers camera.zoom world units.
    public static float drawRadius(CelestialBody body, float visualRadiusScale, float zoom) {
        return Math.max(MIN_BODY_PIXELS * zoom, body.getRadius() / visualRadiusScale);
    }

    public Renderer() {
        shape = new ShapeRenderer();
        shape.setAutoShapeType(true);
    }

    public void render(OrthographicCamera camera, Array<CelestialBody> bodies, float visualRadiusScale) {
        shape.setProjectionMatrix(camera.combined);

        shape.begin(ShapeRenderer.ShapeType.Line);
        for (CelestialBody body : bodies) {
            Array<Vector2> trail = body.getTrail();
            if (trail.size < 2) continue;

            for (int i = 0; i < trail.size - 1; i++) {
                float t = (float) i / (trail.size - 1); // 0 = oldest, 1 = newest
                Color base = body.getColor();
                float alpha = 0.05f + 0.95f * t;
                shape.setColor(base.r, base.g, base.b, alpha);

                Vector2 a = trail.get(i);
                Vector2 b = trail.get(i + 1);
                shape.line(a.x, a.y, b.x, b.y);
            }
        }
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (CelestialBody body : bodies) {
            Color c = body.getColor();
            shape.setColor(c);
            Vector2 p = body.getPosition();
            shape.circle(p.x, p.y, drawRadius(body, visualRadiusScale, camera.zoom), 32);
        }
        shape.end();
    }

    public void dispose() {
        shape.dispose();
    }
}
