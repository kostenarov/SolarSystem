package io.kostenarov.SolarSystem;

import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class Planet extends Actor {
    private String name;
    private float distanceFromSun; // in million kilometers
    private float diameter; // in tens thousands kilometers
    private float orbitalPeriod; // in Earth days
    private Circle texture;

    public Planet(String name, float distanceFromSun, float diameter, float orbitalPeriod) {
        this.name = name;
        this.distanceFromSun = distanceFromSun;
        this.diameter = diameter;
        this.orbitalPeriod = orbitalPeriod;
        this.texture = new Circle(300, 300, diameter / 2); // Assuming the texture is a circle with radius half of the diameter
    }

    public String getName() {
        return name;
    }

    public float getDistanceFromSun() {
        return distanceFromSun;
    }

    public float getDiameter() {
        return diameter;
    }

    public float getOrbitalPeriod() {
        return orbitalPeriod;
    }

    public Circle getTexture() {
        return texture;
    }
}
