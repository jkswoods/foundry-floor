# Configuration and access

Version 0.2.0 has no player-facing configuration file, power requirements, bench tiers or upgrade modules. Install the mod and enable it for the intended world.

## Ownership

The player placing a Foundry block becomes its owner. That owner controls mechanical actions and removal. Crafting at the Foundry Workbench uses the native crafting interface; it is not an inventory container with configurable permissions. Multiplayer was included in player testing.

Elevator controls and panel linking are deferred. Older elevator records remain readable for compatibility and owner dismantling. There is no administrative ownership-transfer command in this preview.

## World data

The mod saves ownership, occupied-cell reservations, control states and inert older-preview records in the world's Foundry resource. Keep world and player files with their matching mod builds when making backups. The current reader preserves the earlier ownership/state format rather than resetting it.

No custom global key bindings or client settings are required. Use the game's normal placement, rotation, removal and Use actions.

## Audio

Original generated sounds use bounded spatial attenuation for mechanical actions. Retired elevator parts play no movement loop. Bench opening and closing use local interface sounds. This version has no separate mod volume slider; use Hytale's sound controls. The sounds were included in player listening checks.
