package io.kostenarov.SolarSystem.render;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * Background stars drawn in screen space from a mesh uploaded once, so they cost one draw call
 * and stay visible at any zoom. They drift slowly as the camera moves, wrapping at the edges.
 */
public class StarField {
    private static final int STAR_COUNT = 2000;
    // Stars move this fraction of the distance the view scrolls on screen
    private static final double PARALLAX = 0.1;

    private static final String VERTEX_SHADER =
        "attribute vec2 a_position;\n" +
        "attribute float a_brightness;\n" +
        "uniform vec2 u_offset;\n" +
        "varying float v_brightness;\n" +
        "void main() {\n" +
        "    vec2 p = fract(a_position - u_offset);\n" +
        "    gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);\n" +
        "    v_brightness = a_brightness;\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#ifdef GL_ES\n" +
        "precision mediump float;\n" +
        "#endif\n" +
        "varying float v_brightness;\n" +
        "void main() {\n" +
        "    gl_FragColor = vec4(vec3(v_brightness), 1.0);\n" +
        "}\n";

    private final Mesh mesh;
    private final ShaderProgram shader;

    public StarField() {
        shader = new ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (!shader.isCompiled()) {
            throw new GdxRuntimeException("Star shader failed to compile: " + shader.getLog());
        }

        // Positions are fractions of the screen; brightness is skewed so most stars are faint
        float[] vertices = new float[STAR_COUNT * 3];
        for (int i = 0; i < STAR_COUNT; i++) {
            vertices[i * 3] = MathUtils.random();
            vertices[i * 3 + 1] = MathUtils.random();
            float r = MathUtils.random();
            vertices[i * 3 + 2] = 0.25f + 0.75f * r * r * r;
        }

        mesh = new Mesh(true, STAR_COUNT, 0,
            new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_brightness"));
        mesh.setVertices(vertices);
    }

    public void render(OrthographicCamera camera) {
        // Offset in screen fractions, computed in double so it stays smooth far from the origin
        double offsetX = camera.position.x / camera.zoom * PARALLAX / camera.viewportWidth;
        double offsetY = camera.position.y / camera.zoom * PARALLAX / camera.viewportHeight;

        shader.bind();
        shader.setUniformf("u_offset", (float) (offsetX - Math.floor(offsetX)), (float) (offsetY - Math.floor(offsetY)));
        mesh.render(shader, GL20.GL_POINTS);
    }

    public void dispose() {
        mesh.dispose();
        shader.dispose();
    }
}
