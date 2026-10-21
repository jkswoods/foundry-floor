# Foundry Floor

Original factory architecture and mechanical blocks for **Hytale 0.7.3 / Java 25**. The collection contains a dedicated Foundry Workbench and 27 factory blocks: connected steel grates, catwalks, wider stairs, safety edges, walls, roofs, windows, industrial lighting, vents, ducts, pallets, doors, shutters and lighting.

Craft the bench at a stock Workbench with 8 Iron Ingots and 4 Copper Ingots. Its five tabs group Floors, Walkways, Structure, Fixtures and Mechanisms. See the [player guide](docs/index.md), [recipes](docs/recipes.md), [architecture](docs/features/architecture.md).

## Development status

The 0.2.0 repair preview is undergoing player testing. Native startup checks 28 craftable item links and 27 bench recipes. Elevators, guide extensions and call panels have been deferred to a future release. Hidden inert assets retain older-world compatibility and owner dismantling; they expose no recipes, controls, passenger motion or empty-shaft reservation. Ordinary gameplay, audio and multiplayer verification continue.

The earlier 0.1.0 development candidate targets 0.7.2. Keep historical candidate files separate from the current 0.7.3 preview. No tested final release or public distribution license has been selected. Repository access is private: [jkswoods/foundry-floor](https://github.com/jkswoods/foundry-floor).

## Build

Supply a separately installed JDK 25 and the official Hytale 0.7.3 server JAR. Neither game binaries nor stock assets are redistributed.

```sh
FF_JAVA_HOME=/path/to/jdk25 FF_SERVER_JAR=/path/to/HytaleServer.jar bash tools/build.sh
```

The build runs focused core and pinned native collision checks, compiles the plugin and writes `build/libs/FoundryFloor-0.2.0-dev.jar` with deterministic ZIP ordering/timestamps. The checked-in native assets are sufficient for the main build; it does not regenerate audio. Run `bash tools/build.sh --core` for the core tests alone.

## Authoring and documentation

`tools/build_models.py` and `tools/build_connections.py` generate the original active assets; `tools/retire_elevator.py` maintains inert compatibility records. Editable Blockbench models and part specifications are in `art/source/`; runtime assets are in `assets/`. One block is32 model units. Python authoring uses Pillow and NumPy. The active authoring kit contains28 craftable designs,150 connection variants and8 mechanism clips.

Runtime sound families were generated through the connected ElevenLabs Sound Effects workflow. Sanitized selected-take metadata is in `assets/SOURCE-AUDIO.json`; raw recordings and private development evidence are excluded from the repository.

Install the pinned documentation dependency with `python3 -m pip install -r requirements-docs.txt`, then run `bash tools/build_player_guide.sh`. Set `FF_DOCS_PYTHON` to select an existing Python environment. Output is `build/player-site/`. The branded guide includes installation, recipes, connections, controls, ownership and known limits. Current native media and acceptance are being completed. No public documentation host is enabled.

`python3 tools/package_player_guide.py` creates a deployable guide archive. `python3 tools/package_authoring.py` creates a separate editable model kit. These ZIPs are not game mods. `python3 tools/package_development.py --source-commit COMMIT_SHA` bundles reviewed artifacts and checksums into a versioned private candidate; a candidate is separate from the final tested gameplay release.

Back up worlds and player data with matching JARs. The 0.2.0 reader migrates older ownership records and retains earlier FFR1–FFR4 records, including retired content. To downgrade, restore the matching complete backup rather than changing the JAR alone.
