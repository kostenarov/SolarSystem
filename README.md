# Solar System Simulator

A visually stunning, real-time n-body physics simulation of celestial systems. Create, simulate, and visualize planetary orbits with accurate Newtonian gravity. Built with **libGDX** and **Java**, featuring interactive controls, custom system definitions, and moon orbital mechanics.

## Features

✨ **Core Capabilities**
- **Accurate N-Body Physics**: Implements Newton's law of universal gravitation for realistic orbital mechanics
- **Real-Time Visualization**: Watch celestial bodies orbit and interact in real-time with smooth 60 FPS rendering
- **Customizable Systems**: Load pre-made planetary systems or create your own via JSON configuration files
- **Interactive Camera Controls**: Pan, zoom, and follow individual bodies around the simulation
- **Orbital Trails**: Visualize the paths of planets and moons as they orbit
- **Planetary Rings**: Some celestial bodies can be equipped with visually stunning ring systems

🎮 **User Interface**
- **Intuitive Start Screen**: Select from multiple pre-configured systems or start custom simulations
- **Live Body Information**: Click on bodies in the UI to view their properties (name, mass, radius, position, velocity)
- **Simulation Controls**: Pause/resume simulation, toggle visual elements (stars, trails, rings)
- **Body Selection**: Follow any celestial body and track its motion through space
- **Responsive Controls**: Full keyboard and mouse support for navigation and interaction

## Getting Started

### Prerequisites
- **Java Development Kit (JDK)**: Version 8 or higher
- **Gradle**: The project uses Gradle for build management (wrapper included)

### Installation

1. **Clone or download the repository:**
   ```bash
   git clone https://github.com/kostenarov/SolarSystem.git
   cd SolarSystem
   ```

2. **Build the project:**
   ```bash
   ./gradlew build
   ```

3. **Run the application:**
   ```bash
   ./gradlew lwjgl3:run
   ```

   Or use the compiled JAR:
   ```bash
   java -jar lwjgl3/build/libs/SolarSystem-1.0.0.jar
   ```

## Controls

### Camera Navigation (when not following a body)
- **A** - Pan left
- **D** - Pan right
- **W** - Pan up
- **S** - Pan down
- **Q** - Zoom in
- **E** - Zoom out

### Simulation Control
- **F** - Toggle follow mode (tracks the selected body)
- **R** - Reset camera to the Sun

### UI Interaction
- **Click on bodies** in the left panel to select them for tracking
- **Pause/Resume** button to control simulation playback
- **Toggle Options** for displaying stars, moons trails, and other visual elements

## System Configuration

### Predefined Systems
The project comes with several pre-configured systems in the `saves/` directory:

- **exampleSystem.json** - A realistic solar system with the Sun, planets, and moons
- **3BodyProblem.json** - A classic three-body chaotic system
- **AsteroidsSystem.json** - An asteroid field simulation
- **randomSystem.json** - A procedurally generated random system

### Creating Custom Systems

Systems are defined in JSON format. Each system is an array of celestial body objects:

```json
[
  {
    "name": "Sun",
    "mass": 1.989e30,
    "radius": 100.9634e11,
    "color": [1.0, 0.95, 0.0, 1.0],
    "position": [0.0, 0.0],
    "velocity": [0.0, 700.0],
    "hasRings": false,
    "isMoon": false
  },
  {
    "name": "Earth",
    "mass": 5.972e24,
    "radius": 6.371e6,
    "color": [0.0, 0.0, 1.0, 1.0],
    "position": [1.496e11, 0.0],
    "velocity": [0.0, 29780.0],
    "hasRings": false,
    "isMoon": false
  }
]
```

**Property Details:**
- **name**: Display name of the celestial body
- **mass**: Mass in kilograms (uses real astronomical values)
- **radius**: Radius in meters
- **color**: RGBA color array (values 0.0-1.0)
- **position**: X,Y position as [x, y] in meters or AU
- **velocity**: X,Y velocity as [vx, vy] in m/s
- **hasRings**: Boolean flag for ring rendering
- **isMoon**: Boolean flag to mark moons (affects trail display by default)

Save your custom JSON file to the `saves/` directory and select it from the start screen.

## Project Structure

```
SolarSystem/
├── core/                          # Core physics and logic
│   ├── src/main/java/
│   │   └── io/kostenarov/SolarSystem/Classes/
│   │       ├── CelestialBody.java        # Body representation
│   │       ├── PhysicsEngine.java        # N-body physics simulation
│   │       ├── BodyInitializer.java      # System loading from JSON
│   │       ├── SolarSystemVisualiser.java # Main renderer
│   │       ├── Renderer.java             # Graphics rendering
│   │       ├── UIInitializer.java        # UI management
│   │       └── StartScreen.java          # Start screen UI
│   └── src/test/java/                    # Unit tests
├── lwjgl3/                        # LWJGL3 desktop implementation
│   ├── src/main/java/
│   │   └── io/kostenarov/SolarSystem/lwjgl3/
│   │       └── Lwjgl3Launcher.java       # Application entry point
│   └── src/main/resources/               # UI assets
├── assets/                        # Game assets (textures, etc.)
├── saves/                         # Pre-configured system definitions
└── README.md                      # This file
```

## Technical Details

### Physics Simulation
The simulator uses Newton's law of universal gravitation:

```
F = G * (m₁ * m₂) / r²
```

Where:
- **G** = 6.67430 × 10⁻¹¹ (gravitational constant)
- **m₁, m₂** = masses of the two bodies
- **r** = distance between bodies

The acceleration of each body is calculated by:

```
a = F / m
```

### Time Step
The default simulation runs at 2 minutes of simulation time per frame, allowing for interesting orbital dynamics while maintaining stability.

### Rendering
- Uses **libGDX** for cross-platform graphics
- **ShapeRenderer** for efficient 2D geometry drawing
- **OrthographicCamera** for intuitive 2D space navigation
- Procedurally generated star field for visual depth

## Testing

The project includes unit tests for core physics calculations:

```bash
./gradlew test
```

Test files are located in `core/src/test/java/` and cover:
- **CelestialBodyTests.java** - Body property validation
- **PhysicsEngineTests.java** - Physics calculations and orbital mechanics

## Troubleshooting

### Issue: "NoSuchMethodError: java.nio.file.Files.readString"
**Solution:** Ensure you're using Java 8 or higher. The project requires at least JDK 8 for compatibility with libGDX.

### Issue: Application crashes on startup
**Solution:** Verify that the `saves/` directory exists and contains valid JSON system files. Check the console output for detailed error messages.

### Issue: Performance is slow
**Solution:** 
- Reduce the number of bodies in your simulation
- Decrease the star count or disable star rendering
- Lower the zoom level to reduce rendering complexity

## Dependencies

### Build Tools
- **Gradle 8.x** - Build automation
- **Java 8+** - Runtime environment

### Libraries
- **libGDX 1.12+** - Cross-platform game framework
- **LWJGL 3** - Lightweight Java Game Library bindings
- **libGDX UI** - Scene2D UI framework

See `build.gradle` for complete dependency list.

## Future Enhancements

Potential features for future development:
- Collision detection and merging of bodies
- Custom skybox textures and planet textures
- Relativistic effects for high-speed scenarios
- Gravitational lensing visualization
- Export simulation data to files
- Network multiplayer simulation viewing
- Advanced orbital mechanics (Lagrange points, escape velocity calculator)

## License

This project is provided as-is for educational and entertainment purposes.

## Author

Created by **Mitko Kostenarov**

## Contributing

Found a bug or have a feature suggestion? Feel free to create an issue or submit improvements!

---

## Quick Start Examples

### Example 1: Run the Default Solar System
1. Launch the application: `./gradlew lwjgl3:run`
2. Click "Start" to begin the default Earth-centered simulation
3. Press **F** to follow Earth's orbit
4. Use **Q** and **E** to zoom in/out

### Example 2: Create a Binary Star System
Create `saves/binaryStars.json`:
```json
[
  {
    "name": "Star A",
    "mass": 1.989e30,
    "radius": 6.963e8,
    "color": [1.0, 1.0, 0.0, 1.0],
    "position": [-1e11, 0.0],
    "velocity": [0.0, 30000.0],
    "hasRings": false,
    "isMoon": false
  },
  {
    "name": "Star B",
    "mass": 1.989e30,
    "radius": 6.963e8,
    "color": [1.0, 0.7, 0.0, 1.0],
    "position": [1e11, 0.0],
    "velocity": [0.0, -30000.0],
    "hasRings": false,
    "isMoon": false
  }
]
```

Then load it from the start screen!

Enjoy exploring the cosmos! 🌍🪐⭐
