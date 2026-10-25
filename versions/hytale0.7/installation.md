---
title: "Installation"
---

# Installation {#installation}

This public alpha targets **Hytale 0.7.3** and **Java 25**. [Download 0.2.0-dev.2](https://github.com/jkswoods/foundry-floor/releases/tag/v0.2.0-dev.2) or browse the [source repository](https://github.com/jkswoods/foundry-floor).

## Install the alpha {#install-the-alpha}

1. Back up the complete test world and player data together with the matching mod JARs.
2. Close that world or stop its server normally.
3. Put one `FoundryFloor-0.2.0-dev.jar` in Hytale's `UserData/Mods` folder. Remove the older Foundry JAR when updating so only one version remains. On macOS, this is `~/Library/Application Support/Hytale/UserData/Mods/`.
4. Create a separate test world and enable **FoundryFloor** in that world's Mods tab.
5. Check the server log for the Foundry Floor startup message. It checks the native item links, workbench recipes and mechanism assets.

For a dedicated server, place the same JAR in its configured mods directory. The server needs its normal Hytale authentication setup. Offline dedicated connections are rejected by Hytale; offline mode is limited to single-player.

No other mod is required. Multiplayer was included in player testing. Compatibility with individual automation mods and large server setups is not guaranteed. Keep a single Foundry version installed.

## Build from source {#build-from-source}

Supply your own JDK 25 and the official Hytale 0.7.3 server JAR:

```sh
FF_JAVA_HOME=/path/to/jdk25 FF_SERVER_JAR=/path/to/HytaleServer.jar bash tools/build.sh
```

The build runs the focused checks and creates `build/libs/FoundryFloor-0.2.0-dev.jar`. Game binaries and stock assets are separate dependencies.

## Update or remove {#update-or-remove}

Save and stop the world first. Back up the world/player files with the current JAR before replacing it. Do not downgrade a world by changing the JAR alone: restore its matching backup if needed.

Remove placed Foundry blocks while the mod is still installed. Elevator parts from earlier previews are inert; their owner can dismantle the controller and attached cab without lowering it first. Keep the cab area loaded while dismantling. Save, stop, then remove the JAR. Removing a mod before its blocks can leave missing content; automatic world cleanup is not supplied.

Continue to [first factory bay](getting-started.md).
