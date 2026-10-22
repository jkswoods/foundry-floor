# Changelog

## Unreleased — door glass and building support repairs

- Move the complete personnel door, including its translucent pane, through matching whole-door poses during opening and closing.
- Declare native support faces on structural surfaces so workbenches and supported furniture can be placed on them.
- Align all catwalk deck variants with the top of the block grid and drain grates; furniture no longer needs to sit above a bottom-aligned thin deck.
- Keep roof collision continuous at its full-block surface. Existing catwalks move within their cells; check older stair landings after updating.
- Add native support-matching and shipped-model regression checks. This draft candidate awaits player testing.
- Remove obsolete invisible upper cells left by older safety-edge and floor models, while preserving the floor itself and neighbouring blocks. This repair also runs for already placed connected safety floors when their chunks load.

## 0.2.0-dev.2 — public alpha, Hytale 0.7.3

- Published the source, player guide and versioned prerelease downloads.
- Added a manually dispatched GitHub Pages workflow with build-only documentation pull-request checks.
- Completed player QA for remaining mechanisms, crafting/building, normal save/reload, sounds and multiplayer.

- Added connected grates, safety-floor corners, wider stair flights, large vents and pallet squares.
- Corrected moving model groups, shutter dimensions and held scale, stair landing bounds, placement previews and duct rotation.
- Added steel walls, corrugated roofs, framed windows, thin panes and high-bay pendant lighting.
- Deferred elevators, guide extensions, call panels and floor controls to a future release. Existing placed parts are inert and owner-dismantleable, with no empty-shaft reservation.
- Added an original connected ElevenLabs glass-handling cue with four retained takes.
- Repaired legacy invisible shutter/stair fillers, doorway swing bounds and catwalk landings in copied native test scenes.
- Added glass alpha blending, controls on both shutter faces and a geometry audit for flickering surfaces across all model variants.
- Corrected the client animation format and per-face glass transparency flags. Added a real glazed door opening, matching finishes on both door faces and all six wall faces, and the complete elbow rotation set.
- Retained original model renders in the guide; current gameplay screenshots are not included yet.

## 0.1.0 — earlier development preview

- Added the original factory collection: 25 block models and a dedicated Foundry Workbench.
- Added five bench tabs with native recipe definitions and verified native bench/catalog transaction quantities.
- Added server-owned placement reservations, mechanical controls and saved ownership/state.
- Implemented an experimental bounded freight-lift controller with cab collision poses, call-panel links and saved passenger records.
- Verified empty lift motion, entity obstruction, and held-height normal reload in isolated native server checks. Passenger riding remains experimental.
- Added original generated mechanical and workshop sound families.
- Added the player-guide draft and reproducible source/build structure.

0.2.0-dev.2 is the current public alpha. The earlier development previews remain historical releases.
