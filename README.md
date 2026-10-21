# Foundry Floor

Original modular factory architecture and mechanical blocks for Hytale0.7.2.

The collection contains26 original models: the dedicated Foundry Workbench and25 factory floor, catwalk, structure, fixture and mechanism blocks. Craft the bench at a stock Workbench with8 iron bars and4 copper bars; its recipe groups are Floors, Walkways, Structure, Fixtures and Mechanisms. Opening a placed bench and crafting a freight lift have been tested in game. Complete crafting/placement acceptance is still in progress.

This private development repository contains original Hytale/Blockbench assets, native recipes, server-owned placement/mechanical controls and an experimental bounded freight-lift controller with held collision poses, native passenger velocity, call-panel links and saved passenger records. Native crafting, controls and passenger travel acceptance remain in progress. No tested playable release has been published.

The player-guide draft covers the bench, recipes, building layouts, controls, installation and current limitations. Native usage screenshots are still pending. Install MkDocs1.6.1 using `python3 -m pip install -r requirements-docs.txt`, then run `bash tools/build_player_guide.sh`; output is `build/player-site/`. This fixes the documentation build timestamp through `SOURCE_DATE_EPOCH` for reproducible archives. Set `FF_DOCS_PYTHON` to choose a project Python environment. `python3 tools/package_player_guide.py` creates a separate deployable guide preview archive with notices. No public docs host is enabled.

## Reproducible build

Supply a separately installed JDK25 and the official Hytale0.7.2 server JAR. Neither game binaries nor game assets are redistributed.

```sh
FF_JAVA_HOME=/path/to/jdk25 FF_SERVER_JAR=/path/to/HytaleServer.jar bash tools/build.sh
```

The build runs the focused core tests, compiles the native adapter and emits `build/libs/FoundryFloor-0.1.0-dev.jar` with deterministic ZIP ordering/timestamps. `bash tools/build.sh --core` runs the core tests alone.

For model authoring alone, `python3 tools/package_authoring.py` creates a separate model/rig archive. It contains editable Blockbench projects and exported native geometry/animations; it is not an installable gameplay mod.

Original editable models are in `art/source/`; native runtime assets are in `assets/`. Model authoring tools use Python with Pillow and NumPy. The main build uses the checked-in native assets and does not regenerate paid audio. Runtime audio was generated through the connected ElevenLabs Sound Effects workflow; sanitized source metadata is retained in `assets/SOURCE-AUDIO.json`.

The target is Hytale0.7.2, server revision `d769dc3d57881261a62858e1394f0e4de9a21eca`, Java25. This is a private development project; no public distribution license or production support claim has been selected.
