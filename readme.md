# FluidLogged

[![Build status](https://github.com/MrFuzzihead/FluidLogged/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/MrFuzzihead/FluidLogged/actions/workflows/build-and-test.yml)
[![Latest release](https://img.shields.io/github/v/release/MrFuzzihead/FluidLogged?include_prereleases&sort=semver)](https://github.com/MrFuzzihead/FluidLogged/releases/latest)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.7.10-62a34a)](https://minecraft.wiki/w/Java_Edition_1.7.10)
[![Forge](https://img.shields.io/badge/Forge-10.13.4.1614-1e2b4f)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.7.10.html)
[![License](https://img.shields.io/badge/License-LGPL--3.0-yellow)](COPYING.LESSER)

**FluidLogged** is a waterlogging backport for **Minecraft 1.7.10 (Forge)** that extends waterlogging to Forge fluids with block representations. This repository is a continuation of the original [FluidLogged](https://github.com/GTMEGA/FluidLogged) project, maintained with a modernized 1.7.10 build toolchain and ongoing compatibility fixes.

> FluidLogged targets the Forge 1.7.10 environment. If a fluid or block should be supported but is not, please open a [compatibility request](https://github.com/MrFuzzihead/FluidLogged/issues/new?template=compat-request.yml).

## Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Downloads](#downloads)
- [Building from source](#building-from-source)
- [Contributing](#contributing)
- [Changes in this fork](#changes-in-this-fork)
- [Roadmap](#roadmap)
- [Credits](#credits)
- [License](#license)

## Features

- Waterlogs compatible blocks with water and Forge fluids that have block versions
- Preserves fluidlogged state in world data and synchronizes it between the client and server
- Supports bucket interactions and fluid simulation for compatible fluid blocks
- Handles the standard 1.7.10 waterloggable block set, including stairs, slabs, fences, panes, ladders, rails, signs, trapdoors, and chests
- Provides APIs for registering additional bucket and world drivers

## Requirements

| Dependency                                                         | Version      | Notes                                                                                   |
|--------------------------------------------------------------------|--------------|-----------------------------------------------------------------------------------------|
| Minecraft                                                          | 1.7.10       | —                                                                                       |
| Minecraft Forge                                                    | 10.13.4.1614 | [Download](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.7.10.html) |
| [ChunkAPI](https://github.com/FalsePattern/ChunkAPI)               | 0.6.4+       | Required at runtime                                                                     |
| [FalsePatternLib](https://github.com/FalsePattern/FalsePatternLib) | 1.7.0+       | Required at runtime                                                                     |

## Installation

1. Install **Minecraft Forge 10.13.4.1614** for Minecraft 1.7.10.
2. Download the latest **FluidLogged** jar from the [Releases](https://github.com/MrFuzzihead/FluidLogged/releases/latest) page.
3. Download the matching 1.7.10 versions of [ChunkAPI](https://github.com/FalsePattern/ChunkAPI) and [FalsePatternLib](https://github.com/FalsePattern/FalsePatternLib).
4. Drop the FluidLogged and dependency jars into your `.minecraft/mods` folder.
5. Launch Minecraft using the Forge profile.

## Downloads

- **[GitHub Releases](https://github.com/MrFuzzihead/FluidLogged/releases/latest)** — recommended for stable builds.
- **[GitHub Actions](https://github.com/MrFuzzihead/FluidLogged/actions)** — snapshot builds from pushes to the default branch.

## Building from source

This project uses the [GTNewHorizons](https://github.com/GTNewHorizons) 1.7.10 Gradle toolchain. The Gradle wrapper provisions a compatible JDK, so no manual Java setup is required.

```bash
git clone https://github.com/MrFuzzihead/FluidLogged.git
cd FluidLogged
./gradlew build        # on Windows: gradlew.bat build
```

The built jars are placed in `build/libs/`. To launch a development client:

```bash
./gradlew runClient    # on Windows: gradlew.bat runClient
```

## Contributing

Bug reports and pull requests are welcome! Please use the [issue tracker](https://github.com/MrFuzzihead/FluidLogged/issues) for bugs and compatibility questions.

- Use a descriptive branch name, such as `bugfix-<issue>-<short-name>` or `feature-<issue>-<short-name>`.
- Run `./gradlew build` before opening a pull request.
- CI runs the build, Spotless, and Checkstyle checks for changes targeting the default branch.

## Changes in this fork

This fork continues the original FluidLogged project while modernizing its development workflow:

- Migrated to the GTNewHorizons 1.7.10 Gradle toolchain and Gradle wrapper
- Added GitHub Actions build/test and release workflows
- Enabled Spotless and Checkstyle checks as part of the build
- Updated build configuration and dependencies for the current toolchain
- Added optional Angelica/Celeritas renderer integration without a published runtime dependency

## Roadmap

- Correct light propagation
- Add blast-resistance behavior
- Validate Angelica/Celeritas and Iris rendering
- Keep OptiFine and other render-replacing mods out of scope

## Credits

- **FluidLogged** by FalsePattern and the MEGA Team
- **[ChunkAPI](https://github.com/FalsePattern/ChunkAPI)** for chunk data storage and synchronization
- **[FalsePatternLib](https://github.com/FalsePattern/FalsePatternLib)** for the 1.7.10 library support used by the mod
- **[GTNewHorizons](https://github.com/GTNewHorizons)** for the 1.7.10 build tooling

## License

FluidLogged is licensed under the [GNU Lesser General Public License v3](COPYING.LESSER). See [LICENSE](LICENSE) for the project notice.
