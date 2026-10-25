---
title: "Troubleshooting"
---

# Troubleshooting {#troubleshooting}

## Which file do I install? {#which-file-do-i-install}

Download `FoundryFloor-0.2.0-dev.jar` from the [0.2.0-dev.2 release](https://github.com/jkswoods/foundry-floor/releases/tag/v0.2.0-dev.2). The candidate ZIP contains the same JAR and an offline guide. The authoring ZIP is for model editing and is not a game mod.

## The Foundry Workbench recipe is missing {#the-foundry-workbench-recipe-is-missing}

Check that **FoundryFloor** is enabled for this world and that the server is Hytale 0.7.3. Look in the standard Workbench's crafting category. The recipe requires **8 Iron Ingots and 4 Copper Ingots**. The 27 factory recipes belong to the new bench rather than the stock Workbench.

If the plugin's startup checks fail, retain the log and the exact JAR version. Do not mix multiple Foundry JARs.

## A block will not place {#a-block-will-not-place}

Clear the complete assembly footprint, including adjoining cells and overhead pieces. Load nearby chunks and avoid another Foundry mechanism's reserved space. Large models may occupy cells beyond their visible centre.

The current repair preview clears obsolete invisible fillers left by older Foundry shutter and stair shapes when their surrounding chunks load. Keep the assembly and its neighbours loaded briefly, then retry the side, top or landing cell. Do not remove the original assembly just to clear these cells.

## Use does nothing {#use-does-nothing}

Check the displayed Use key, distance and ownership. Wait for the mechanism's cooldown. Clear its movement space and nearby entities. Check that the game window has focus, and try the same Use action on a stock Workbench before changing any mod settings.

## Where is the elevator? {#where-is-the-elevator}

The elevator, guide extensions and call panels have been deferred to a future release. They have no recipes or creative catalogue entries. Older placed parts load as inert compatibility objects: they do not move, open controls, play a movement loop or reserve an empty shaft. Their owner can dismantle the controller and attached cab while the area is loaded. Old inventory parts cannot be placed again.

## I cannot remove an assembly {#i-cannot-remove-an-assembly}

Only its owner can remove it. For a retired elevator, keep the area loaded and remove its controller pedestal; its attached cab is removed with it. Restore a matching backup if a test world has lost its controller or ownership data. There is no destructive cleanup command in this preview.

## My local dedicated connection is rejected {#my-local-dedicated-connection-is-rejected}

Hytale rejects offline authentication for dedicated-server connections. Use the server's normal authentication setup or test in single-player. Do not change another server's authentication settings for this mod.

## Current limits {#current-limits}

This alpha has passed player checks for crafting, placement, mechanisms, normal save/reload, sounds and multiplayer. Other Hytale versions, arbitrary mod combinations and large production servers are not certified. Keep matching world/player backups and report reproducible issues.

Report reproducible mod issues in the [repository issue tracker](https://github.com/jkswoods/foundry-floor/issues), including the mod version, Hytale version and a short reproduction. Remove personal details and tokens from logs before attaching them.
