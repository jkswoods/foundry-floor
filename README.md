# Foundry Floor

[Player guide](https://jkswoods.github.io/foundry-floor/) · [Download the alpha](https://github.com/jkswoods/foundry-floor/releases/tag/v0.2.0-dev.2)

![Foundry Floor — Build the factory around your machines](docs/assets/brand/banner.svg)

**Build the factory around your machines.** Original factory architecture and mechanical blocks for **Hytale 0.7.3 / Java 25**, with a shared steel, brass and safety-yellow palette.

![Foundry Floor assembly with steel floors, elevated catwalks, stairs, columns, shutter and Foundry Workbench](docs/assets/models/factory-assembly.png)

*Original model render showing the factory building pieces together.*

- A dedicated Foundry Workbench and **27 factory blocks**.
- Connected steel grates, catwalks, wider stairs and safety edges.
- Walls, roofs, windows and bolted steel frames for the factory shell.
- Doors, shutters, a service hatch and industrial lighting.
- Vents, utility ducts and cargo pallets to finish the workshop.

Craft the bench at a stock Workbench with **8 Iron Ingots and 4 Copper Ingots**. Its five tabs group Floors, Walkways, Structure, Fixtures and Mechanisms. Ducts and vents are architectural props.

[Install](docs/installation.md) · [Your first factory bay](docs/getting-started.md) · [Recipes](docs/recipes.md) · [Architecture](docs/features/architecture.md) · [Mechanisms](docs/features/mechanisms.md)

## The Foundry Workbench in Hytale

![Foundry Workbench with its steel worktop, tool board, drawer and vice, captured in Hytale](docs/assets/native/foundry-workbench.png)

*Native capture from the earlier 0.1.0 development build on Hytale 0.7.2. The workbench uses the same original factory design; current 0.7.3 guide captures are still pending.*

## Development status

This **0.2.0 public alpha** targets Hytale 0.7.3 / Java 25. Player testing covered crafting, placement, mechanical controls, normal save/reload, sounds and multiplayer. Automated checks cover recipe registration, collision bounds and older-world compatibility. Keep matching world/player backups; this prerelease does not promise compatibility with other game versions or every mod combination.

Elevators, guide extensions and call panels are deferred to a future release. Older placed parts remain inert and owner-dismantleable, with no controls, passenger movement or empty-shaft reservation. They cannot be crafted or placed again.

The earlier 0.1.0 candidate targets Hytale 0.7.2. Use the 0.2.0 downloads for 0.7.3. No redistribution license has been selected; third-party notices remain in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Build

Supply a separately installed JDK 25 and the official Hytale 0.7.3 server JAR. Neither game binaries nor stock assets are redistributed.

```sh
FF_JAVA_HOME=/path/to/jdk25 FF_SERVER_JAR=/path/to/HytaleServer.jar bash tools/build.sh
```

The build runs focused core and pinned native collision checks, compiles the plugin and writes `build/libs/FoundryFloor-0.2.0-dev.jar` with deterministic ZIP ordering/timestamps. The checked-in native assets are sufficient for the main build; it does not regenerate audio. Run `bash tools/build.sh --core` for the core tests alone.

## Authoring and documentation

`tools/build_models.py` and `tools/build_connections.py` generate the original active assets; `tools/retire_elevator.py` maintains inert compatibility records. Editable Blockbench models and part specifications are in `art/source/`; runtime assets are in `assets/`. One block is32 model units. Python authoring uses Pillow and NumPy. The active authoring kit contains28 craftable designs,150 connection variants and8 mechanism clips.

Runtime sound families were generated through the connected ElevenLabs Sound Effects workflow. Sanitized selected-take metadata is in `assets/SOURCE-AUDIO.json`; raw recordings and private development evidence are excluded from the repository.

Install the pinned documentation dependency with `python3 -m pip install -r requirements-docs.txt`, then run `bash tools/build_player_guide.sh`. Set `FF_DOCS_PYTHON` to select an existing Python environment. Output is `build/player-site/`. The branded guide includes installation, recipes, connections, controls, ownership and known limits. The online guide is published at https://jkswoods.github.io/foundry-floor/ through GitHub Pages. Its workflow builds docs pull requests without deploying, and publishes from `main` on manual dispatch. The guide uses original model renders; this README also includes the earlier native Workbench capture above.

`python3 tools/package_player_guide.py --public-alpha` creates a deployable guide archive. `python3 tools/package_authoring.py` creates a separate editable model kit. These ZIPs are not game mods. `python3 tools/package_development.py --source-commit COMMIT_SHA --version 0.2.0-dev.2 --public-alpha` bundles the reviewed runtime and guide with checksums.

Back up worlds and player data with matching JARs. The 0.2.0 reader migrates older ownership records and retains earlier FFR1–FFR4 records, including retired content. To downgrade, restore the matching complete backup rather than changing the JAR alone.
