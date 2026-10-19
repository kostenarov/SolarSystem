// language: java
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.kostenarov.SolarSystem.Classes.CelestialBody;
import io.kostenarov.SolarSystem.Classes.PhysicsEngine;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PhysicsEngineTests {

    private static final float EPS = 1e-2f;
    private PhysicsEngine physicsEngine;

    @Before
    public void setup() {

    }

    @Test
    public void singleBody_noAcceleration_positionUpdatedByVelocityOnly() {
        Array<CelestialBody> bodies = new Array<>();
        CelestialBody single = new CelestialBody("Single", 1e20f, 1e3f, Color.WHITE,
            new Vector2(0f, 0f), new Vector2(1000f, 0f));
        bodies.add(single);

        PhysicsEngine engine = new PhysicsEngine(bodies);

        float dt = 3600f; // 1 hour
        Vector2 posBefore = new Vector2(single.getPosition());
        Vector2 velBefore = new Vector2(single.getVelocity());

        engine.update(dt);

        Vector2 posAfter = single.getPosition();
        Vector2 velAfter = single.getVelocity();

        // velocity should remain effectively unchanged (no external forces)
        assertEquals(velBefore.x, velAfter.x, EPS);
        assertEquals(velBefore.y, velAfter.y, EPS);

        // position should have moved by vel * dt
        float expectedX = posBefore.x + velBefore.x * dt;
        float expectedY = posBefore.y + velBefore.y * dt;
        assertEquals(expectedX, posAfter.x, 1e-1f * Math.max(1f, Math.abs(expectedX)));
        assertEquals(expectedY, posAfter.y, 1e-1f * Math.max(1f, Math.abs(expectedY)));
    }

    @Test
    public void update_zeroTimeStep_noChange() {
        Array<CelestialBody> bodies = new Array<>();
        CelestialBody b = new CelestialBody("B", 1e20f, 1e3f, Color.WHITE,
            new Vector2(1e6f, -2e6f), new Vector2(500f, -300f));
        bodies.add(b);

        PhysicsEngine engine = new PhysicsEngine(bodies);

        Vector2 posBefore = new Vector2(b.getPosition());
        Vector2 velBefore = new Vector2(b.getVelocity());

        engine.update(0f);

        assertEquals(posBefore.x, b.getPosition().x, EPS);
        assertEquals(posBefore.y, b.getPosition().y, EPS);
        assertEquals(velBefore.x, b.getVelocity().x, EPS);
        assertEquals(velBefore.y, b.getVelocity().y, EPS);
    }

    @Test
    public void twoBodies_gravity_decreasesDistance() {
        Array<CelestialBody> bodies = new Array<>();

        float separationHalf = 1.0e7f; // 10 million meters from center
        float mass = 1.0e22f;
        CelestialBody left = new CelestialBody("Left", mass, 1e3f, Color.GRAY,
            new Vector2(-separationHalf, 0f), new Vector2(0f, 0f));
        CelestialBody right = new CelestialBody("Right", mass, 1e3f, Color.GRAY,
            new Vector2(separationHalf, 0f), new Vector2(0f, 0f));

        bodies.add(left);
        bodies.add(right);

        PhysicsEngine engine = new PhysicsEngine(bodies);

        float dt = 3600f; // 1 hour
        float distBefore = left.getPosition().dst(right.getPosition());

        engine.update(dt);

        float distAfter = left.getPosition().dst(right.getPosition());

        // gravitational attraction should reduce separation
        assertTrue("Distance should decrease after gravity update", distAfter < distBefore);
    }
}
