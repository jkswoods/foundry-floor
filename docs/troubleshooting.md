# Troubleshooting

## I cannot find a download

There is no tested playable release yet. The [private repository](https://github.com/jkswoods/foundry-floor) is available only to collaborators. Do not substitute the older model-kit archive for the gameplay plugin.

## The Foundry Workbench recipe is missing

Check that **FoundryFloor** is enabled for this world and that the server is Hytale 0.7.2. Look in the standard Workbench's crafting category. The recipe requires **8 Iron Ingots and 4 Copper Ingots**. The 25 factory recipes belong to the new bench rather than the stock Workbench.

If the plugin's startup checks fail, retain the log and the exact JAR version. Do not mix multiple Foundry JARs.

## A block will not place

Clear the complete assembly footprint, including adjoining cells and overhead pieces. Load nearby chunks and avoid another Foundry mechanism's reserved space. Large models may occupy cells beyond their visible centre. The lift needs its console space and a clear shaft behind it. Move everyone out of a new lift's shaft before placement.

## Use does nothing

Check the displayed Use key, distance and ownership. Wait for the mechanism's cooldown. Clear its movement space and nearby entities. Check that the game window has focus, and try the same Use action on a stock Workbench before changing any mod settings.

## A lift is Blocked or Disabled

Leave its reserved shaft clear and load the whole assembly. Saved passengers must be online and safely supported. Clear the obstruction, then let the owner request a dock again. Reloading holds a partially travelled cab instead of moving it automatically. Passenger recovery remains an experimental behaviour that still needs native verification.

## I cannot remove an assembly

Only its owner can remove it. A lift must be lowered and empty; remove its controller pedestal, not the internal cab. Restore a matching backup if a test world has lost its controller or ownership data. There is no destructive cleanup command in this preview.

## My local dedicated connection is rejected

Hytale rejects offline authentication for dedicated-server connections. Use the server's normal authentication setup or test in single-player. Do not change another server's authentication settings for this mod.

## Current limits

In-game recipe quantities, Foundry placement/rotation/walking, mechanical controls, save/reload, passenger carrying, human audio approval and real multiplayer acceptance remain open. Loader and automated checks establish a development baseline, not a survival or production support claim.

Report reproducible mod issues in the [repository issue tracker](https://github.com/jkswoods/foundry-floor/issues), including the mod version, Hytale version and a short reproduction. Remove personal details and tokens from logs before attaching them.
