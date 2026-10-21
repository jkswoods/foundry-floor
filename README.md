# Foundry Floor

Original modular factory architecture and mechanical blocks for Hytale0.7.2.

The collection contains26 original models: the dedicated Foundry Workbench and25 factory floor, catwalk, structure, fixture and mechanism blocks. Craft the bench at a stock Workbench with8 iron bars and4 copper bars; its recipe groups are Floors, Walkways, Structure, Fixtures and Mechanisms. Native crafting/placement acceptance is still in progress.

This private development repository currently contains the original Hytale/Blockbench assets, native recipes, the gameplay-plugin foundation and focused automated tests. Server-backed ownership, bounded placement claims and mechanical controls are under integration. The passenger freight-lift controller, native acceptance and complete player guide/site are not delivered yet. No tested playable release has been published at this stage.

## Reproducible build

Supply a separately installed JDK25 and the official Hytale0.7.2 server JAR. Neither game binaries nor game assets are redistributed.

```sh
FF_JAVA_HOME=/path/to/jdk25 FF_SERVER_JAR=/path/to/HytaleServer.jar bash tools/build.sh
```

The build runs the focused core tests, compiles the native adapter and emits `build/libs/FoundryFloor-0.1.0-dev.jar` with deterministic ZIP ordering/timestamps. `bash tools/build.sh --core` runs the core tests alone.

Original editable models are in `art/source/`; native runtime assets are in `assets/`. Model authoring tools use Python with Pillow and NumPy. The main build uses the checked-in native assets and does not regenerate paid audio. Runtime audio was generated through the connected ElevenLabs Sound Effects workflow; sanitized source metadata is retained in `assets/SOURCE-AUDIO.json`.

The target is Hytale0.7.2, server revision `d769dc3d57881261a62858e1394f0e4de9a21eca`, Java25. This is a private development project; no public distribution license or production support claim has been selected.
