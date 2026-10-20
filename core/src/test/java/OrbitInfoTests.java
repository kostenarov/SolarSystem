import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import io.kostenarov.SolarSystem.physics.CelestialBody;
import io.kostenarov.SolarSystem.physics.OrbitInfo;
import io.kostenarov.SolarSystem.physics.PhysicalConstants;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OrbitInfoTests {
    private static final float SUN_MASS = 1.989e30f;
    private static final float EARTH_MASS = 5.972e24f;

    @Test
    public void circularOrbit_hasZeroEccentricityAndKeplerPeriod() {
        double r = PhysicalConstants.AU;
        double mu = PhysicalConstants.G * (SUN_MASS + (double) EARTH_MASS);
        float circular = (float) Math.sqrt(mu / r);

        OrbitInfo orbit = OrbitInfo.of(earth(circular), sun());

        assertTrue(orbit.bound);
        assertEquals(r, orbit.distance, 1e4);
        assertEquals(circular, orbit.speed, 0.01);
        assertEquals(0, orbit.eccentricity, 1e-4);
        assertEquals(r, orbit.semiMajorAxis, r * 1e-4);
        double year = 2 * Math.PI * Math.sqrt(r * r * r / mu);
        assertEquals(year, orbit.period, year * 1e-4);
        assertEquals(365.25, orbit.period / 86400, 0.5); // about one Earth year
    }

    @Test
    public void fasterThanEscapeSpeed_isUnbound() {
        double escape = Math.sqrt(2 * PhysicalConstants.G * SUN_MASS / PhysicalConstants.AU);
        OrbitInfo orbit = OrbitInfo.of(earth((float) (escape * 1.1)), sun());
        assertFalse(orbit.bound);
        assertTrue(orbit.eccentricity > 1);
    }

    @Test
    public void slowerThanCircular_isAnEllipseStartingAtAphelion() {
        double r = PhysicalConstants.AU;
        double circular = Math.sqrt(PhysicalConstants.G * (SUN_MASS + (double) EARTH_MASS) / r);
        OrbitInfo orbit = OrbitInfo.of(earth((float) (circular * 0.9)), sun());

        assertTrue(orbit.bound);
        assertTrue(orbit.semiMajorAxis < r);
        // At aphelion r = a(1 + e)
        assertEquals(r, orbit.semiMajorAxis * (1 + orbit.eccentricity), r * 1e-4);
    }

    private static CelestialBody sun() {
        return new CelestialBody("Sun", SUN_MASS, 6.957e8f, Color.YELLOW, new Vector2(0, 0), new Vector2(0, 0));
    }

    private static CelestialBody earth(float speed) {
        return new CelestialBody("Earth", EARTH_MASS, 6.371e6f, Color.BLUE, new Vector2(PhysicalConstants.AU, 0), new Vector2(0, speed));
    }
}
