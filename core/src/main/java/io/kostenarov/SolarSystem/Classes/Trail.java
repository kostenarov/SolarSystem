package io.kostenarov.SolarSystem.Classes;

/**
 * The recent positions of a body, oldest first, in a ring buffer: adding a point when full
 * overwrites the oldest one instead of shifting every point along.
 */
public class Trail {
    private static final int INITIAL_CAPACITY = 1024;

    private float[] xs = new float[0];
    private float[] ys = new float[0];
    private int start = 0; // index of the oldest point
    private int size = 0;
    private int maxPoints;

    public Trail(int maxPoints) {
        this.maxPoints = Math.max(1, maxPoints);
    }

    public int size() {
        return size;
    }

    public float getX(int i) {
        return xs[(start + i) % xs.length];
    }

    public float getY(int i) {
        return ys[(start + i) % ys.length];
    }

    public void clear() {
        start = 0;
        size = 0;
    }

    /** Changes how many points are kept; shrinking drops the oldest points straight away. */
    public void setMaxPoints(int maxPoints) {
        maxPoints = Math.max(1, maxPoints);
        if (maxPoints == this.maxPoints) return;
        this.maxPoints = maxPoints;
        if (xs.length > maxPoints) resize(maxPoints);
    }

    public void add(float x, float y) {
        if (size > 0) {
            int last = (start + size - 1) % xs.length;
            if (Math.abs(xs[last] - x) <= 0.001f && Math.abs(ys[last] - y) <= 0.001f) return;
        }

        // Grow by doubling until the buffer reaches maxPoints, so short trails stay small
        if (size == xs.length && xs.length < maxPoints) {
            resize(Math.min(maxPoints, Math.max(INITIAL_CAPACITY, xs.length * 2)));
        }

        if (size < xs.length) {
            int end = (start + size) % xs.length;
            xs[end] = x;
            ys[end] = y;
            size++;
        } else {
            xs[start] = x;
            ys[start] = y;
            start = (start + 1) % xs.length;
        }
    }

    // Copies the newest min(size, capacity) points into a new buffer, oldest first
    private void resize(int capacity) {
        int keep = Math.min(size, capacity);
        float[] newXs = new float[capacity];
        float[] newYs = new float[capacity];
        for (int i = 0; i < keep; i++) {
            newXs[i] = getX(size - keep + i);
            newYs[i] = getY(size - keep + i);
        }
        xs = newXs;
        ys = newYs;
        start = 0;
        size = keep;
    }
}
