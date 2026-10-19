package io.kostenarov.SolarSystem.Classes;

public class BodyConfig {
    public String name;
    public double mass;
    public double radius;
    public float[] color;
    public float[] position;
    public float[] velocity;
    public boolean hasRings = false;
    public boolean isMoon = false;
    public String orbitParent;
    public double orbitRadius;
    public float orbitAngleDegrees = 0f;
}
