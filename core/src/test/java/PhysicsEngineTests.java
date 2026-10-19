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

    @Test
    public void verlet_conservesEnergyBetterThanEuler() {
        double eulerError = maxRelativeEnergyErrorOverOneYear(PhysicsEngine.Integrator.SEMI_IMPLICIT_EULER);
        double verletError = maxRelativeEnergyErrorOverOneYear(PhysicsEngine.Integrator.VELOCITY_VERLET);

        assertTrue("Verlet energy error " + verletError + " should be far below Euler's " + eulerError,
            verletError < eulerError / 10);
    }

    @Test
    public void update_withSubsteps_matchesManualSmallSteps() {
        Array<CelestialBody> substepped = sunAndEarth();
        Array<CelestialBody> manual = sunAndEarth();
        PhysicsEngine substeppedEngine = new PhysicsEngine(substepped, PhysicsEngine.Integrator.VELOCITY_VERLET, 600);
        PhysicsEngine manualEngine = new PhysicsEngine(manual, PhysicsEngine.Integrator.VELOCITY_VERLET, Double.POSITIVE_INFINITY);

        substeppedEngine.update(3600);
        for (int i = 0; i < 6; i++) manualEngine.update(600);

        for (int i = 0; i < substepped.size; i++) {
            assertEquals(manual.get(i).getPosition().x, substepped.get(i).getPosition().x, EPS);
            assertEquals(manual.get(i).getPosition().y, substepped.get(i).getPosition().y, EPS);
        }
    }

    // Earth on a circular orbit, stepped one day at a time for a year
    private static double maxRelativeEnergyErrorOverOneYear(PhysicsEngine.Integrator integrator) {
        Array<CelestialBody> bodies = sunAndEarth();
        PhysicsEngine engine = new PhysicsEngine(bodies, integrator, Double.POSITIVE_INFINITY);

        double initialEnergy = totalEnergy(bodies);
        double maxError = 0;
        for (int day = 0; day < 365; day++) {
            engine.update(86400);
            maxError = Math.max(maxError, Math.abs((totalEnergy(bodies) - initialEnergy) / initialEnergy));
        }
        return maxError;
    }

    private static Array<CelestialBody> sunAndEarth() {
        float au = 1.496e11f;
        float sunMass = 1.989e30f;
        float orbitalSpeed = (float) Math.sqrt(PhysicsEngine.G * sunMass / au);

        Array<CelestialBody> bodies = new Array<>();
        bodies.add(new CelestialBody("Sun", sunMass, 7e8f, Color.YELLOW, new Vector2(0f, 0f), new Vector2(0f, 0f)));
        bodies.add(new CelestialBody("Earth", 5.972e24f, 6.371e6f, Color.BLUE, new Vector2(au, 0f), new Vector2(0f, orbitalSpeed)));
        return bodies;
    }

    private static double totalEnergy(Array<CelestialBody> bodies) {
        double energy = 0;
        for (int i = 0; i < bodies.size; i++) {
            CelestialBody a = bodies.get(i);
            energy += 0.5 * a.getMass() * a.getVelocity().len2();
            for (int j = i + 1; j < bodies.size; j++) {
                CelestialBody b = bodies.get(j);
                energy -= PhysicsEngine.G * a.getMass() * b.getMass() / a.getPosition().dst(b.getPosition());
            }
        }
        return energy;
    }
}
