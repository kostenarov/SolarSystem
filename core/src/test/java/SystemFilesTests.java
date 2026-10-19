import io.kostenarov.SolarSystem.system.BodyConfig;
import io.kostenarov.SolarSystem.system.SystemFiles;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SystemFilesTests {

    @Test
    public void toJson_thenFromJson_keepsEveryField() {
        BodyConfig[] original = {star(), moon("Moon", "Sun")};
        original[0].hasRings = true;

        BodyConfig[] loaded = SystemFiles.fromJson(SystemFiles.toJson(original));

        assertEquals(2, loaded.length);
        assertEquals("Sun", loaded[0].name);
        assertEquals(1.989e30, loaded[0].mass, 1e24);
        assertEquals(6.957e8, loaded[0].radius, 1);
        assertArrayEquals(new float[]{1f, 0.9f, 0f, 1f}, loaded[0].color, 1e-6f);
        assertArrayEquals(new float[]{0.5f, -1f}, loaded[0].position, 1e-6f);
        assertArrayEquals(new float[]{10f, 700f}, loaded[0].velocity, 1e-6f);
        assertTrue(loaded[0].hasRings);

        assertEquals("Sun", loaded[1].orbitParent);
        assertEquals(3.844e8, loaded[1].orbitRadius, 1);
        assertEquals(30f, loaded[1].orbitAngleDegrees, 1e-6f);
        assertTrue(loaded[1].isMoon);
    }

    @Test
    public void toJson_writesPlainJsonWithoutClassTags() {
        String json = SystemFiles.toJson(new BodyConfig[]{star()});
        assertTrue(json.trim().startsWith("["));
        assertTrue(json.contains("\"name\": \"Sun\"") || json.contains("\"name\":\"Sun\""));
        assertFalse(json.contains("\"class\""));
    }

    @Test
    public void validate_acceptsAValidSystem() {
        assertNull(SystemFiles.validate(new BodyConfig[]{star(), moon("Moon", "Sun")}));
    }

    @Test
    public void validate_rejectsCommonMistakes() {
        assertNotNull("empty", SystemFiles.validate(new BodyConfig[0]));

        BodyConfig noMass = star();
        noMass.mass = 0;
        assertNotNull("zero mass", SystemFiles.validate(new BodyConfig[]{noMass}));

        assertNotNull("duplicate name", SystemFiles.validate(new BodyConfig[]{star(), star()}));
        assertNotNull("missing parent", SystemFiles.validate(new BodyConfig[]{star(), moon("Moon", "Earth")}));
        assertNotNull("moon of a moon", SystemFiles.validate(new BodyConfig[]{star(), moon("A", "Sun"), moon("B", "A")}));
    }

    @Test
    public void isValidSaveName_rejectsPathCharacters() {
        assertTrue(SystemFiles.isValidSaveName("My system_2"));
        assertFalse(SystemFiles.isValidSaveName("../evil"));
        assertFalse(SystemFiles.isValidSaveName("a/b"));
        assertFalse(SystemFiles.isValidSaveName("   "));
    }

    private static BodyConfig star() {
        BodyConfig c = new BodyConfig();
        c.name = "Sun";
        c.mass = 1.989e30;
        c.radius = 6.957e8;
        c.color = new float[]{1f, 0.9f, 0f, 1f};
        c.position = new float[]{0.5f, -1f};
        c.velocity = new float[]{10f, 700f};
        return c;
    }

    private static BodyConfig moon(String name, String parent) {
        BodyConfig c = new BodyConfig();
        c.name = name;
        c.mass = 7.342e22;
        c.radius = 1.737e6;
        c.color = new float[]{0.8f, 0.8f, 0.8f, 1f};
        c.isMoon = true;
        c.orbitParent = parent;
        c.orbitRadius = 3.844e8;
        c.orbitAngleDegrees = 30f;
        return c;
    }
}
