# Configuration and access

Version 0.1.0 has no player-facing configuration file, power requirements, bench tiers or upgrade modules. Install the mod and enable it for the intended world.

## Ownership

The player placing a Foundry block becomes its owner. That owner controls mechanical actions and removal. Crafting at the Foundry Workbench uses the native crafting interface; it is not an inventory container with configurable permissions. Multiplayer access is still undergoing verification.

Lift panels link only to an owned lift. Passenger tickets and panel links are saved with world ownership and mechanical state. There is no administrative ownership-transfer command in this preview.

## World data

The mod saves ownership, occupied-cell reservations, control states, lift height, passenger records and panel links in the world's Foundry resource. Keep world and player files with their matching mod builds when making backups. The current reader preserves the earlier ownership/state format rather than resetting it.

No custom global key bindings or client settings are required. Use the game's normal placement, rotation, removal and Use actions.

## Audio

Original generated sounds use bounded spatial attenuation for mechanical actions. The lift loop is tied to the controller's moving states. Bench opening and closing use local interface sounds. This version has no separate mod volume slider; use Hytale's sound controls. Human listening acceptance remains pending.
