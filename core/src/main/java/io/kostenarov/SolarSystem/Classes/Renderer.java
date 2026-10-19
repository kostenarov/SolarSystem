package io.kostenarov.SolarSystem.Classes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Renderer {
    private final ShapeRenderer shape;

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
            float drawRadius = Math.max(1f, body.getRadius() / visualRadiusScale);
            shape.circle(p.x, p.y, drawRadius, 32);
        }
        shape.end();
    }

    public void dispose() {
        shape.dispose();
    }
}
