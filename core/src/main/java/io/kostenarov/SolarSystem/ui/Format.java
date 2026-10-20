package io.kostenarov.SolarSystem.ui;

import io.kostenarov.SolarSystem.physics.PhysicalConstants;

import java.util.Locale;

/** Human-readable quantities for the UI. Always uses '.' as the decimal point, whatever the system locale. */
public final class Format {
    private static final double EARTH_MASS = 5.972e24;
    private static final double SOLAR_MASS = 1.989e30;
    private static final double DAY = 86400;
    private static final double YEAR = 365.25 * DAY;

    private Format() {
    }

    public static String duration(double seconds) {
        if (Double.isInfinite(seconds)) return "-";
        if (seconds < 60) return f("%.0f s", seconds);
        if (seconds < 3600) return f("%.1f min", seconds / 60);
        if (seconds < DAY) return f("%.1f h", seconds / 3600);
        if (seconds < 2 * YEAR) return f("%.1f days", seconds / DAY);
        return f("%.2f years", seconds / YEAR);
    }

    /** Elapsed simulated time as "123 days" or "4 y 56 d". */
    public static String elapsed(double seconds) {
        long days = (long) (seconds / DAY);
        if (days < 365) return days + (days == 1 ? " day" : " days");
        long years = (long) (seconds / YEAR);
        return years + " y " + (long) ((seconds - years * YEAR) / DAY) + " d";
    }

    /** Kilometres below 0.01 AU, AU above. */
    public static String distance(double metres) {
        if (metres < 0.01 * PhysicalConstants.AU) return f("%,.0f km", metres / 1000);
        return f("%.3f AU", metres / PhysicalConstants.AU);
    }

    public static String speed(double metresPerSecond) {
        return f("%.2f km/s", metresPerSecond / 1000);
    }

    public static String mass(double kg) {
        if (kg >= 0.01 * SOLAR_MASS) return f("%.3g kg (%.3g Suns)", kg, kg / SOLAR_MASS);
        return f("%.3g kg (%.3g Earths)", kg, kg / EARTH_MASS);
    }

    public static String radius(double metres) {
        return f("%,.0f km", metres / 1000);
    }

    public static String f(String format, Object... args) {
        return String.format(Locale.US, format, args);
    }
}
