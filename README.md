# Orbital Railgun (Forge 1.20.1)

This repository contains the Forge 1.20.1 port of the original Fabric **orbital-railgun** mod.  It preserves the gameplay loop of charging the orbital railgun, requesting a strike from orbit, and rendering the large-scale destruction and HUD effects using Forge client events and vanilla post-processing.

## Getting Started

1. Install the Minecraft Forge 1.20.1 MDK toolchain (Java 17).
2. Clone this repository and run the Gradle wrapper to generate the run configurations:

   ```bash
   ./gradlew genIntellijRuns   # or genEclipseRuns / genVSCodeRuns
   ```

3. Launch the game from your IDE or via `./gradlew runClient`.

All resources, including Geo models, textures, and shaders, live under `src/main/resources/assets/orbital_railgun/`.

## Development Notes

* The railgun item is registered via `DeferredRegister` in `ModItems` and enforces a full charge before firing.
* Networking is handled with a `SimpleChannel` in `NetworkHandler`; see the `net.msg` package for the client/server payloads.
* Client-only state and rendering live under `com.mishkis.orbitalrailgun.client`, with post-processing handled by vanilla `PostChain` instances instead of Satin.
* Server-side strike logic (entity knockback, damage, and terrain carving) is managed by `OrbitalRailgunStrikeManager`.

## License

This port retains the original MIT license of the Fabric project.
