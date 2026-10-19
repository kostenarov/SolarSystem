import io.kostenarov.SolarSystem.physics.Trail;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TrailTests {

    @Test
    public void add_keepsPointsOldestFirst() {
        Trail trail = new Trail(10);
        for (int i = 0; i < 5; i++) trail.add(i, -i);

        assertEquals(5, trail.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(i, trail.getX(i), 0f);
            assertEquals(-i, trail.getY(i), 0f);
        }
    }

    @Test
    public void add_whenFull_dropsTheOldestPoint() {
        Trail trail = new Trail(3);
        for (int i = 0; i < 5; i++) trail.add(i, 0);

        assertEquals(3, trail.size());
        assertEquals(2, trail.getX(0), 0f);
        assertEquals(4, trail.getX(2), 0f);
    }

    @Test
    public void add_skipsRepeatedPoint() {
        Trail trail = new Trail(10);
        trail.add(1, 1);
        trail.add(1, 1);
        assertEquals(1, trail.size());
    }

    @Test
    public void add_growsPastInitialCapacity() {
        Trail trail = new Trail(5000);
        for (int i = 0; i < 3000; i++) trail.add(i, 0);

        assertEquals(3000, trail.size());
        assertEquals(0, trail.getX(0), 0f);
        assertEquals(2999, trail.getX(2999), 0f);
    }

    @Test
    public void setMaxPoints_shrinking_keepsNewestPoints() {
        Trail trail = new Trail(100);
        for (int i = 0; i < 50; i++) trail.add(i, 0);

        trail.setMaxPoints(10);

        assertEquals(10, trail.size());
        assertEquals(40, trail.getX(0), 0f);
        assertEquals(49, trail.getX(9), 0f);
    }

    @Test
    public void setMaxPoints_afterWrapping_keepsOrder() {
        Trail trail = new Trail(4);
        for (int i = 0; i < 7; i++) trail.add(i, 0); // buffer has wrapped: holds 3,4,5,6

        trail.setMaxPoints(2);

        assertEquals(2, trail.size());
        assertEquals(5, trail.getX(0), 0f);
        assertEquals(6, trail.getX(1), 0f);
    }
}
