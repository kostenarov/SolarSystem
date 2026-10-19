package io.kostenarov.SolarSystem.Classes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.ObjectSet;

/** Reads, writes and validates solar system save files (a JSON array of {@link BodyConfig}). */
public final class SystemFiles {
    public static final String SAVES_DIR = "saves";
    public static final String EXTENSION = ".json";

    private SystemFiles() {
    }

    public static String toJson(BodyConfig[] configs) {
        Json json = new Json(JsonWriter.OutputType.json);
        return json.prettyPrint(json.toJson(configs, BodyConfig[].class));
    }

    public static BodyConfig[] fromJson(String text) {
        BodyConfig[] configs = new Json().fromJson(BodyConfig[].class, text);
        if (configs == null) throw new IllegalArgumentException("The file does not contain a list of bodies");
        return configs;
    }

    /** Save names (without extension) found in the saves folder, sorted. */
    public static Array<String> listSaves() {
        Array<String> names = new Array<>();
        FileHandle dir = Gdx.files.local(SAVES_DIR);
        if (!dir.isDirectory()) return names;
        for (FileHandle file : dir.list(EXTENSION)) {
            names.add(file.nameWithoutExtension());
        }
        names.sort();
        return names;
    }

    public static FileHandle saveFile(String name) {
        return Gdx.files.local(SAVES_DIR + "/" + name + EXTENSION);
    }

    public static BodyConfig[] read(String name) {
        return fromJson(saveFile(name).readString("UTF-8"));
    }

    public static void write(String name, BodyConfig[] configs) {
        saveFile(name).writeString(toJson(configs), false, "UTF-8");
    }

    /** Letters, digits, spaces, '-' and '_' only, so the name is always a safe file name. */
    public static boolean isValidSaveName(String name) {
        return name != null && name.trim().length() > 0 && name.matches("[A-Za-z0-9 _-]+");
    }

    /** Returns a description of the first problem found, or null if the system can be simulated. */
    public static String validate(BodyConfig[] configs) {
        if (configs == null || configs.length == 0) return "The system has no bodies";

        ObjectSet<String> names = new ObjectSet<>();
        for (BodyConfig c : configs) {
            if (c.name == null || c.name.trim().isEmpty()) return "Every body needs a name";
            if (!names.add(c.name)) return "Two bodies are called \"" + c.name + "\"";
            if (!(c.mass > 0)) return c.name + ": mass must be greater than 0";
            if (!(c.radius > 0)) return c.name + ": radius must be greater than 0";
        }

        for (BodyConfig c : configs) {
            if (!c.orbitsParent()) continue;
            BodyConfig parent = find(configs, c.orbitParent);
            if (parent == null) return c.name + ": parent \"" + c.orbitParent + "\" does not exist";
            if (parent == c) return c.name + " cannot orbit itself";
            if (parent.orbitsParent()) return c.name + ": parent " + parent.name + " is itself a moon";
            if (!(c.orbitRadius > 0)) return c.name + ": orbit radius must be greater than 0";
        }
        return null;
    }

    private static BodyConfig find(BodyConfig[] configs, String name) {
        for (BodyConfig c : configs) {
            if (name.equals(c.name)) return c;
        }
        return null;
    }
}
