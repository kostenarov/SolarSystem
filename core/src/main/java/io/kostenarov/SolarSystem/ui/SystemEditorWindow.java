package io.kostenarov.SolarSystem.ui;

import io.kostenarov.SolarSystem.physics.PhysicalConstants;
import io.kostenarov.SolarSystem.system.SystemFiles;
import io.kostenarov.SolarSystem.system.BodyConfig;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;

/**
 * Opened from the start menu: lets the user build a solar system body by body, save it to
 * saves/&lt;name&gt;.json, and start the simulation with it.
 */
public class SystemEditorWindow extends Window {

    public interface Listener {
        /** "Start simulation" was clicked with a valid system. */
        void onSystemApplied(BodyConfig[] configs, String name);

        void onSystemSaved(String name);
    }

    private interface DoubleSetter {
        void set(double value);
    }

    // Moons point at their parent object rather than its name, so renaming a planet keeps its moons
    private static final class EditableBody {
        final BodyConfig config;
        EditableBody parent;

        EditableBody(BodyConfig config) {
            this.config = config;
        }

        boolean isMoon() {
            return parent != null;
        }

        @Override
        public String toString() {
            return isMoon() ? "    " + config.name + "  (moon of " + parent.config.name + ")" : config.name;
        }
    }

    private static final String TYPE_PLANET = "Star / planet";
    private static final String TYPE_MOON = "Moon";
    private static final Color ERROR_COLOR = new Color(1f, 0.45f, 0.45f, 1f);
    private static final Color OK_COLOR = new Color(0.55f, 0.9f, 0.55f, 1f);
    private static final Color INVALID_FIELD_TINT = new Color(1f, 0.5f, 0.5f, 1f);

    private final Skin skin;
    private final Listener listener;
    private final Array<EditableBody> bodies = new Array<>();
    private EditableBody selected;

    private final TextField fileNameField;
    private final List<EditableBody> bodyList;
    private final Table form;
    private final Label statusLabel;

    private String loadedName;       // save the editor contents came from, overwritten without asking
    private String pendingOverwrite; // existing save the user has been warned about once

    public SystemEditorWindow(Skin skin, Listener listener) {
        super("Solar System Editor", skin);
        this.skin = skin;
        this.listener = listener;
        setResizable(false);
        defaults().pad(4);

        // File bar
        fileNameField = new TextField("", skin);
        Table fileBar = new Table();
        fileBar.defaults().padRight(6);
        fileBar.add(new Label("Save as", skin));
        fileBar.add(fileNameField).width(220);
        fileBar.add(button(" Save ", this::save));
        add(fileBar).colspan(2).left();
        row();

        // Body list and its buttons
        bodyList = new List<>(skin);
        bodyList.getSelection().setProgrammaticChangeEvents(false);
        bodyList.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                select(bodyList.getSelected());
            }
        });
        ScrollPane listScroll = new ScrollPane(bodyList, skin);
        listScroll.setFadeScrollBars(false);
        listScroll.setScrollingDisabled(true, false);

        Table listButtons = new Table();
        listButtons.defaults().padRight(4);
        listButtons.add(button(" Add planet ", this::addPlanet));
        listButtons.add(button(" Add moon ", this::addMoon));
        listButtons.add(button(" Remove ", this::removeSelected));

        Table left = new Table();
        left.add(listScroll).width(260).height(340).row();
        left.add(listButtons).padTop(6).left();

        // Properties of the selected body
        form = new Table();
        form.top().left();
        form.defaults().pad(3).left();
        ScrollPane formScroll = new ScrollPane(form, skin);
        formScroll.setFadeScrollBars(false);
        formScroll.setScrollingDisabled(true, false);

        add(left).top();
        add(formScroll).width(420).height(372).top();
        row();

        statusLabel = new Label("", skin);
        statusLabel.setWrap(true);
        Table bottom = new Table();
        bottom.add(statusLabel).width(450).left().expandX();
        bottom.add(button(" Start simulation ", this::apply)).padRight(6);
        bottom.add(button(" Close ", () -> setVisible(false)));
        add(bottom).colspan(2).fillX();

        pack();
    }

    /** Shows the window editing a copy of the given system. */
    public void open(BodyConfig[] system, String name) {
        setStatus("Editing " + name + ".", false);
        setSystem(system, name); // may replace the status with a warning about the file
        show();
    }

    /** Shows the window with a new system containing only a star. */
    public void openNew() {
        newSystem();
        show();
    }

    private void show() {
        setVisible(true);
        toFront();
        if (getStage() != null) {
            setPosition((getStage().getWidth() - getWidth()) / 2, (getStage().getHeight() - getHeight()) / 2);
        }
    }

    // ---- File actions ----

    private void newSystem() {
        bodies.clear();
        BodyConfig sun = new BodyConfig();
        sun.name = "Sun";
        sun.mass = 1.989e30;
        sun.radius = 6.957e8;
        sun.color = new float[]{1f, 0.95f, 0f, 1f};
        sun.position = new float[]{0f, 0f};
        sun.velocity = new float[]{0f, 0f};
        bodies.add(new EditableBody(sun));
        loadedName = null;
        pendingOverwrite = null;
        fileNameField.setText("new-system");
        refreshList();
        select(bodies.first());
        setStatus("Started a new system with a star. Use Add planet to fill it.", false);
    }

    private void save() {
        String name = fileNameField.getText().trim();
        if (!SystemFiles.isValidSaveName(name)) {
            setStatus("Use only letters, digits, spaces, - and _ in the name.", true);
            return;
        }
        BodyConfig[] configs = export();
        String problem = SystemFiles.validate(configs);
        if (problem != null) {
            setStatus("Not saved: " + problem, true);
            return;
        }
        if (SystemFiles.saveFile(name).exists() && !name.equals(loadedName) && !name.equals(pendingOverwrite)) {
            pendingOverwrite = name;
            setStatus("A save called " + name + " already exists. Click Save again to overwrite it.", true);
            return;
        }
        try {
            SystemFiles.write(name, configs);
        } catch (Exception e) {
            setStatus("Could not save: " + e.getMessage(), true);
            return;
        }
        loadedName = name;
        pendingOverwrite = null;
        setStatus("Saved to " + SystemFiles.saveFile(name).path(), false);
        listener.onSystemSaved(name);
    }

    private void apply() {
        BodyConfig[] configs = export();
        String problem = SystemFiles.validate(configs);
        if (problem != null) {
            setStatus("Can't simulate yet: " + problem, true);
            return;
        }
        String name = fileNameField.getText().trim();
        listener.onSystemApplied(configs, name.isEmpty() ? "custom system" : name);
    }

    // ---- Body list ----

    private void setSystem(BodyConfig[] configs, String name) {
        bodies.clear();
        for (BodyConfig c : configs) {
            bodies.add(new EditableBody(normalised(c.copy())));
        }

        StringBuilder orphans = new StringBuilder();
        for (int bIndex = 0; bIndex < bodies.size; bIndex++) {
            EditableBody b = bodies.get(bIndex);
            if (!b.config.orbitsParent()) continue;
            EditableBody parent = findPlanet(b.config.orbitParent);
            if (parent != null && parent != b) {
                b.parent = parent;
            } else {
                orphans.append(orphans.length() == 0 ? "" : ", ").append(b.config.name);
                b.config.orbitParent = null;
            }
        }

        loadedName = name;
        pendingOverwrite = null;
        fileNameField.setText(name != null ? name : "");
        refreshList();
        select(bodies.size > 0 ? bodies.first() : null);
        if (orphans.length() > 0) {
            setStatus("Parent not found, so these became planets: " + orphans, true);
        }
    }

    // Fills in the fields a planet needs, so the form never has to deal with missing values
    private static BodyConfig normalised(BodyConfig c) {
        if (c.name == null) c.name = "";
        if (c.color == null || c.color.length < 3) c.color = new float[]{1f, 1f, 1f, 1f};
        if (c.color.length == 3) c.color = new float[]{c.color[0], c.color[1], c.color[2], 1f};
        if (c.position == null || c.position.length < 2) c.position = new float[]{0f, 0f};
        if (c.velocity == null || c.velocity.length < 2) c.velocity = new float[]{0f, 0f};
        return c;
    }

    private EditableBody findPlanet(String name) {
        for (int bIndex = 0; bIndex < bodies.size; bIndex++) {
            EditableBody b = bodies.get(bIndex);
            if (!b.config.orbitsParent() && name.equals(b.config.name)) return b;
        }
        return null;
    }

    // Shows each planet followed by its moons, and keeps the saved order the same
    private void refreshList() {
        Array<EditableBody> ordered = new Array<>(bodies.size);
        for (int bIndex = 0; bIndex < bodies.size; bIndex++) {
            EditableBody b = bodies.get(bIndex);
            if (b.isMoon()) continue;
            ordered.add(b);
            for (int mIndex = 0; mIndex < bodies.size; mIndex++) {
                EditableBody m = bodies.get(mIndex);
                if (m.parent == b) ordered.add(m);
            }
        }
        bodies.clear();
        bodies.addAll(ordered);
        bodyList.setItems(ordered);
        if (selected != null) bodyList.setSelected(selected);
    }

    private void select(EditableBody body) {
        selected = body;
        if (body != null) bodyList.setSelected(body);
        buildForm();
    }

    private void addPlanet() {
        BodyConfig c = new BodyConfig();
        c.name = uniqueName("Planet");
        c.mass = 5.972e24;
        c.radius = 6.371e6;
        c.color = new float[]{MathUtils.random(0.3f, 1f), MathUtils.random(0.3f, 1f), MathUtils.random(0.3f, 1f), 1f};

        // Half an AU beyond the farthest planet, so it doesn't start on top of another body
        float farthest = 0f;
        for (int bIndex = 0; bIndex < bodies.size; bIndex++) {
            EditableBody b = bodies.get(bIndex);
            if (!b.isMoon()) farthest = Math.max(farthest, (float) Math.hypot(b.config.position[0], b.config.position[1]));
        }
        c.position = new float[]{farthest + 0.5f, 0f};
        c.velocity = new float[]{0f, 0f};

        EditableBody body = new EditableBody(c);
        bodies.add(body);
        if (primaryFor(body) != null) setCircularVelocity(body);
        refreshList();
        select(body);
        setStatus("Added " + c.name + " on a circular orbit.", false);
    }

    private void addMoon() {
        EditableBody parent = null;
        if (selected != null) parent = selected.isMoon() ? selected.parent : selected;
        if (parent == null) {
            for (int bIndex = 0; bIndex < bodies.size; bIndex++) {
                EditableBody b = bodies.get(bIndex);
                if (!b.isMoon()) { parent = b; break; }
            }
        }
        if (parent == null) {
            setStatus("Add a planet first, for the moon to orbit.", true);
            return;
        }

        BodyConfig c = normalised(new BodyConfig());
        c.name = uniqueName("Moon");
        c.mass = 7.342e22;
        c.radius = 1.737e6;
        c.color = new float[]{0.8f, 0.8f, 0.8f, 1f};
        c.orbitRadius = 3.844e8;
        c.orbitParent = parent.config.name;

        EditableBody moon = new EditableBody(c);
        moon.parent = parent;
        bodies.add(moon);
        refreshList();
        select(moon);
        setStatus("Added " + c.name + " orbiting " + parent.config.name + ".", false);
    }

    private void removeSelected() {
        if (selected == null) return;
        EditableBody removed = selected;
        int index = bodies.indexOf(removed, true);

        int moonsRemoved = 0;
        for (int i = bodies.size - 1; i >= 0; i--) {
            if (bodies.get(i).parent == removed) {
                bodies.removeIndex(i);
                moonsRemoved++;
            }
        }
        bodies.removeValue(removed, true);

        selected = null;
        refreshList();
        select(bodies.size == 0 ? null : bodies.get(Math.min(index, bodies.size - 1)));
        setStatus("Removed " + removed.config.name + (moonsRemoved > 0 ? " and its " + moonsRemoved + " moon(s)" : "") + ".", false);
    }

    private String uniqueName(String prefix) {
        for (int n = 1; ; n++) {
            String name = prefix + " " + n;
            boolean taken = false;
            for (int bIndex = 0; bIndex < bodies.size; bIndex++) {
                EditableBody b = bodies.get(bIndex);
                if (name.equals(b.config.name)) { taken = true; break; }
            }
            if (!taken) return name;
        }
    }

    // ---- Properties form ----

    private void buildForm() {
        form.clearChildren();
        if (selected == null) {
            form.add(new Label("No bodies. Use Add planet, or New to start from a star.", skin));
            return;
        }
        final EditableBody body = selected;
        final BodyConfig c = body.config;

        final TextField nameField = new TextField(c.name, skin);
        nameField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                c.name = nameField.getText().trim();
                refreshList();
            }
        });
        addFormRow("Name", nameField);

        final SelectBox<String> typeBox = new SelectBox<>(skin);
        typeBox.setItems(TYPE_PLANET, TYPE_MOON);
        typeBox.setSelected(body.isMoon() ? TYPE_MOON : TYPE_PLANET);
        typeBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                changeType(body, TYPE_MOON.equals(typeBox.getSelected()));
            }
        });
        addFormRow("Type", typeBox);

        addFormRow("Mass (kg)", numberField(Double.toString(c.mass), true, v -> c.mass = v));
        addFormRow("Radius (m)", numberField(Double.toString(c.radius), true, v -> c.radius = v));
        addFormRow("Colour", colourEditor(c));

        final CheckBox ringsBox = new CheckBox(" Has rings", skin);
        ringsBox.setChecked(c.hasRings);
        ringsBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                c.hasRings = ringsBox.isChecked();
            }
        });
        addFormRow("", ringsBox);

        if (body.isMoon()) {
            final SelectBox<String> parentBox = new SelectBox<>(skin);
            Array<String> parents = new Array<>();
            for (int bIndex = 0; bIndex < bodies.size; bIndex++) {
                EditableBody b = bodies.get(bIndex);
                if (!b.isMoon() && b != body) parents.add(b.config.name);
            }
            parentBox.setItems(parents);
            parentBox.setSelected(body.parent.config.name);
            parentBox.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    EditableBody parent = findPlanet(parentBox.getSelected());
                    if (parent != null) {
                        body.parent = parent;
                        refreshList();
                    }
                }
            });
            addFormRow("Orbits", parentBox);
            addFormRow("Orbit radius (m)", numberField(Double.toString(c.orbitRadius), true, v -> c.orbitRadius = v));
            addFormRow("Start angle (deg)", numberField(Float.toString(c.orbitAngleDegrees), false, v -> c.orbitAngleDegrees = (float) v));
            addHint("Moons start on a circular orbit around their parent.");
        } else {
            addFormRow("Position X (AU)", numberField(Float.toString(c.position[0]), false, v -> c.position[0] = (float) v));
            addFormRow("Position Y (AU)", numberField(Float.toString(c.position[1]), false, v -> c.position[1] = (float) v));
            addFormRow("Velocity X (m/s)", numberField(Float.toString(c.velocity[0]), false, v -> c.velocity[0] = (float) v));
            addFormRow("Velocity Y (m/s)", numberField(Float.toString(c.velocity[1]), false, v -> c.velocity[1] = (float) v));
            addFormRow("", button(" Set circular orbit velocity ", () -> {
                if (setCircularVelocity(body)) buildForm();
            }));
            addHint("Sets the velocity for a circular orbit around the heaviest other star or planet.");
        }
    }

    private void addFormRow(String label, Actor widget) {
        form.add(new Label(label, skin)).width(130);
        form.add(widget).width(260).row();
    }

    private void addHint(String text) {
        Label hint = new Label(text, skin);
        hint.setWrap(true);
        hint.setColor(0.6f, 0.6f, 0.6f, 1f);
        hint.setFontScale(0.85f);
        form.add(hint).colspan(2).width(390).row();
    }

    // Applies the value as soon as it parses; otherwise tints the field and keeps the old value
    private TextField numberField(String text, final boolean mustBePositive, final DoubleSetter setter) {
        final TextField field = new TextField(text, skin);
        field.setTextFieldFilter((textField, ch) -> Character.isDigit(ch) || "+-.eE".indexOf(ch) >= 0);
        field.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                double value;
                try {
                    value = Double.parseDouble(field.getText().trim());
                } catch (NumberFormatException e) {
                    value = Double.NaN;
                }
                boolean valid = !Double.isNaN(value) && !Double.isInfinite(value) && (!mustBePositive || value > 0);
                field.setColor(valid ? Color.WHITE : INVALID_FIELD_TINT);
                if (valid) setter.set(value);
            }
        });
        return field;
    }

    private Table colourEditor(final BodyConfig c) {
        final Image swatch = new Image(skin.newDrawable("white"));
        swatch.setColor(c.color[0], c.color[1], c.color[2], 1f);

        Table sliders = new Table();
        String[] channels = {"R", "G", "B"};
        for (int i = 0; i < 3; i++) {
            final int channel = i;
            final Slider slider = new Slider(0, 1, 0.01f, false, skin);
            slider.setValue(c.color[i]);
            slider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    c.color[channel] = slider.getValue();
                    swatch.setColor(c.color[0], c.color[1], c.color[2], 1f);
                }
            });
            sliders.add(new Label(channels[i], skin)).padRight(6);
            sliders.add(slider).width(180).row();
        }

        Table editor = new Table();
        editor.add(sliders);
        editor.add(swatch).size(40).padLeft(10);
        return editor;
    }

    private void changeType(EditableBody body, boolean toMoon) {
        if (toMoon == body.isMoon()) return;
        if (toMoon) {
            for (int bIndex = 0; bIndex < bodies.size; bIndex++) {
                EditableBody b = bodies.get(bIndex);
                if (b.parent == body) {
                    setStatus(body.config.name + " has moons of its own. Remove them or move them to another planet first.", true);
                    buildForm();
                    return;
                }
            }
            EditableBody parent = primaryFor(body);
            if (parent == null) {
                setStatus("There is no other star or planet for " + body.config.name + " to orbit.", true);
                buildForm();
                return;
            }
            body.parent = parent;
            if (!(body.config.orbitRadius > 0)) body.config.orbitRadius = 3.844e8;
        } else {
            // Start the former moon next to where its parent was
            float[] parentPosition = body.parent.config.position;
            body.config.position = new float[]{parentPosition[0], parentPosition[1] + 0.1f};
            body.parent = null;
            setCircularVelocity(body);
        }
        refreshList();
        buildForm();
    }

    /** The heaviest star or planet other than the body itself, or null if there is none. */
    private EditableBody primaryFor(EditableBody body) {
        EditableBody primary = null;
        for (int bIndex = 0; bIndex < bodies.size; bIndex++) {
            EditableBody b = bodies.get(bIndex);
            if (b == body || b.isMoon()) continue;
            if (primary == null || b.config.mass > primary.config.mass) primary = b;
        }
        return primary;
    }

    private boolean setCircularVelocity(EditableBody body) {
        EditableBody primary = primaryFor(body);
        if (primary == null) {
            setStatus("There is nothing for " + body.config.name + " to orbit.", true);
            return false;
        }
        float[] p = body.config.position;
        float[] q = primary.config.position;
        double dx = (p[0] - q[0]) * (double) PhysicalConstants.AU;
        double dy = (p[1] - q[1]) * (double) PhysicalConstants.AU;
        double r = Math.hypot(dx, dy);
        if (r == 0) {
            setStatus(body.config.name + " is at the same position as " + primary.config.name + ". Move it first.", true);
            return false;
        }

        // Perpendicular to the line between them (anticlockwise), on top of the primary's own motion
        double speed = Math.sqrt(PhysicalConstants.G * primary.config.mass / r);
        body.config.velocity = new float[]{
            (float) (primary.config.velocity[0] - dy / r * speed),
            (float) (primary.config.velocity[1] + dx / r * speed)
        };
        setStatus(body.config.name + " now orbits " + primary.config.name + " at " + Math.round(speed) + " m/s.", false);
        return true;
    }

    // ---- Helpers ----

    /** The edited system in save-file form, with moon parents written out by name. */
    private BodyConfig[] export() {
        BodyConfig[] configs = new BodyConfig[bodies.size];
        for (int i = 0; i < bodies.size; i++) {
            EditableBody b = bodies.get(i);
            BodyConfig c = b.config.copy();
            if (b.isMoon()) {
                c.isMoon = true;
                c.orbitParent = b.parent.config.name;
                c.position = null;
                c.velocity = null;
            } else {
                c.isMoon = false;
                c.orbitParent = null;
                c.orbitRadius = 0;
                c.orbitAngleDegrees = 0;
            }
            configs[i] = c;
        }
        return configs;
    }

    private TextButton button(String text, final Runnable action) {
        TextButton button = new TextButton(text, skin);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        return button;
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setColor(error ? ERROR_COLOR : OK_COLOR);
    }
}
