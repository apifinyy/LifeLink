# LifeLink Mod

A Fabric mod for Minecraft 1.21.5.

## Overview
LifeLink is a server-side Minecraft mod that links the lives of all players on a server. When one player dies, everyone dies! The mod was originally created for a private SMP and is perfect for cooperative or challenge-based gameplay.

## Features
- **Shared lives:** All players' lives are linked—if one player dies, all players die.
- **Server-side only:** No client installation required; just add to your server's mods folder.
- **In-game commands:**
  - `/lifelink start` — Start the LifeLink challenge.
  - `/lifelink stop` — Stop the LifeLink challenge.
  - `/lifelink revive` — Revive all players.
  - `/lifelink naturaldeaths` — Enable or disable everyone dying from natural causes (e.g., falling, lava, etc.).
- **Fabric mod loader support for Minecraft 1.21.5.**

## Installation

1. Download the latest release JAR from the [releases](https://github.com/apifiny/LifeLink_1.21.5/releases) page or build it yourself.
2. Place the JAR file in your Minecraft `mods` folder.
3. Make sure you are running Minecraft 1.21.5 with [Fabric Loader](https://fabricmc.net/use/) version 0.15.7 or newer.
4. Launch the game and enjoy!

## Building from Source

1. Clone this repository:
   ```
   git clone https://github.com/apifiny/LifeLink_1.21.5.git
   ```
2. Navigate to the project directory:
   ```
   cd LifeLink_1.21.5
   ```
3. Build the mod using Gradle:
   ```
   ./gradlew build
   ```
   The built JAR will be in `build/libs/`.

## Contributing

Contributions are welcome! Please open issues or pull requests for bug fixes, suggestions, or new features.

## Credits

- [FabricMC](https://fabricmc.net/) for the modding platform.
- [Yarn](https://github.com/FabricMC/yarn) for mappings.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
