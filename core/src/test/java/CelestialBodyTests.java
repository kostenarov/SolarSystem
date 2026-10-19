import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.kostenarov.SolarSystem.physics.CelestialBody;
import io.kostenarov.SolarSystem.physics.PhysicalConstants;
import io.kostenarov.SolarSystem.system.BodyConfig;
import io.kostenarov.SolarSystem.system.BodyInitializer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class CelestialBodyTests {

    @Test
    public void copy_hasSameStateButIndependentVectorsAndEmptyTrail() {
        CelestialBody original = new CelestialBody("A", 1e20f, 1e3f, Color.RED,
            new Vector2(5f, 6f), new Vector2(7f, 8f), true, false);
        original.addTrailPoint(original.getPosition(), 10);

        CelestialBody copy = new CelestialBody(original);

        assertEquals("A", copy.getName());
        assertEquals(5f, copy.getPosition().x, 0f);
        assertEquals(8f, copy.getVelocity().y, 0f);
        assertTrue(copy.hasRings());
        assertNotSame(original.getPosition(), copy.getPosition());
        assertEquals(0, copy.getTrail().size());
    }

    @Test
    public void initializeFromConfigs_placesPlanetsInAuAndMoonsOnCircularOrbits() {
        BodyConfig planet = new BodyConfig();
        planet.name = "Earth";
        planet.mass = 5.972e24;
        planet.radius = 6.371e6;
        planet.position = new float[]{1f, 0f};
        planet.velocity = new float[]{0f, 29780f};

        BodyConfig moon = new BodyConfig();
        moon.name = "Moon";
        moon.mass = 7.342e22;
        moon.radius = 1.737e6;
        moon.orbitParent = "Earth";
        moon.orbitRadius = 3.844e8;
        moon.orbitAngleDegrees = 90f;

        Array<CelestialBody> bodies = new BodyInitializer().initializeFromConfigs(new BodyConfig[]{moon, planet});

        assertEquals(2, bodies.size);
        CelestialBody earth = bodies.get(0);
        CelestialBody luna = bodies.get(1);
        assertEquals("Earth", earth.getName());
        assertEquals(PhysicalConstants.AU, earth.getPosition().x, 1e4f);

        // At 90 degrees the moon is directly "above" Earth, moving in -x relative to it
        assertEquals(earth.getPosition().x, luna.getPosition().x, 1e4f);
        assertEquals(3.844e8f, luna.getPosition().y - earth.getPosition().y, 1e3f);
        double expectedSpeed = Math.sqrt(PhysicalConstants.G * (planet.mass + moon.mass) / moon.orbitRadius);
        assertEquals(-expectedSpeed, luna.getVelocity().x - earth.getVelocity().x, 1);
        assertEquals(0, luna.getVelocity().y - earth.getVelocity().y, 1);
        assertTrue(luna.isMoon);
    }
}
