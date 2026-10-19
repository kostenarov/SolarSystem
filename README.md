# SolarSystem

A passion project that aims to create a simple simulation of a solar system.

A 2D n-body gravity simulator built with [libGDX](https://libgdx.com/). Every body pulls on every other body. You can watch the default system, or build your own in the editor, save it as JSON, and run it.

## Running

You need a JDK (17 or newer works). From the project root:

```bash
./gradlew lwjgl3:run
```

On Windows, use `gradlew.bat lwjgl3:run`. To build a runnable jar in `lwjgl3/build/libs/`:

```bash
./gradlew lwjgl3:jar
```

Run the jar from the project root so it finds the `saves/` folder.

## Start menu

The app opens on a menu where you can:

- **Start simulation**: run the selected save (double-clicking a save does the same).
- **Edit selected system** or **Create new system**: open the system editor (see below).
- **Settings**: simulation speed and display options (see below).

## System editor

- Add stars, planets and moons. For each one you can set its name, mass, radius, colour and rings.
- **Planets and stars** take a starting position (AU) and velocity (m/s). **Set circular orbit velocity** fills in the speed for a circular orbit around the heaviest other body.
- **Moons** take a parent, an orbit radius (m) and a starting angle. They start on a circular orbit around their parent.
- The map on the right shows where everything starts and the paths the planets will follow, predicted with a quick simulation. It updates as you type. The selected planet's arrow shows its starting velocity: it is 40 px long at circular-orbit speed. Clicking a body selects it.
- **Save** writes `saves/<name>.json`. **Start simulation** runs the system as it is, saved or not.

## Controls in the simulation

| Input | Action |
| --- | --- |
| Mouse drag / WASD | Pan |
| Mouse wheel / Q, E | Zoom (the wheel zooms towards the cursor) |
| F | Follow the body nearest the centre of the screen, or stop following |
| R | Re-centre on the heaviest body |
| Select Body | Follow a body from the list |
| Pause button | Pause or resume |
| Integrator button | Switch between Verlet, Euler, or both. In **Both** mode, the Euler run is drawn underneath with hollow bodies and dimmer trails, so you can see the two methods drift apart |
| Menu | Back to the start menu |

## Settings

These apply immediately and are kept between runs.

- **Simulation speed**: simulated time per real second.
- **Integration step**: the longest single physics step. Smaller steps are more accurate and slower. Large steps make Euler fail first.
- **Trail length**: how many trail points each body keeps.
- **Body size**: drawn radius as a multiple of the real radius. ×1 is true size.
- **Minimum body size**: the smallest a body is ever drawn, in pixels, so distant planets stay visible.

## Save file format

A save is a JSON array of bodies:

```json
[
  { "name": "Sun", "mass": 1.989e30, "radius": 6.957e8, "color": [1, 0.95, 0, 1],
    "position": [0, 0], "velocity": [0, 0] },
  { "name": "Earth", "mass": 5.972e24, "radius": 6.371e6, "color": [0.2, 0.4, 1, 1],
    "position": [1, 0], "velocity": [0, 29780] },
  { "name": "Moon", "mass": 7.342e22, "radius": 1.737e6, "color": [0.8, 0.8, 0.8, 1],
    "orbitParent": "Earth", "orbitRadius": 3.844e8, "orbitAngleDegrees": 30 }
]
```

| Field | Unit | Notes |
| --- | --- | --- |
| `mass` | kg | Must be above 0 |
| `radius` | m | Must be above 0 |
| `color` | 0–1 | `[r, g, b]` or `[r, g, b, a]` |
| `position` | AU | Stars and planets only |
| `velocity` | m/s | Stars and planets only |
| `hasRings` | | Optional; draws rings around the body |
| `orbitParent` | | Makes the body a moon of the named star or planet |
| `orbitRadius` | m | Moons only |
| `orbitAngleDegrees` | degrees | Moons only; the starting angle around the parent |

Names must be unique, and a moon's parent must not itself be a moon. The menu and the editor check these rules before running or saving a system.

## Code layout

- `physics`: bodies, the gravity engine (Velocity Verlet and semi-implicit Euler, double precision, with sub-stepping), and trails.
- `render`: bodies, trails and the background star field.
- `system`: the save format, loading and saving, and validation.
- `ui`: the in-simulation controls, the settings window, the system editor and its preview.
- `screens`: the start menu and the simulation.
