# Optional Libraries Reference

**Diagram:** `03_optional_libraries.svg`

## Purpose

Diagram `02_module_map.svg` groups Optional libraries by name only; this reference table carries the detail that would overload that diagram: each optional library's purpose and what it depends on, at release `3.32.0-rc.1` (tag `v0.32.0-rc.1`).

---

## Dependency detail

The "Depends on" column is read from each module's `build.gradle` at this commit. It names the in-scope modules first (`com.mapbox.navigationcore:<name>` resolves to public-tree module `<name>`; `project(':x')` is the sibling internal module `x`), then, after a semicolon, the bound SDKs the module declares directly. Mapbox Java SDK sub-artifacts, AndroidX and Kotlin dependencies are omitted. Test configurations are omitted.

Optional libraries that depend on another optional library: `coordination-full-hd` on `coordination`; `adasis` on `weather`; `ev-ui` and `ev-rangemap` on `ev`; `roadcam-ui` on `roadcam`; `core-mapgpt` on `audio`; `ui-mapgpt` and `applemusic-mapgpt` on `core-mapgpt`; `libnavigation-compose-core` on `libnavigation-compose-foundation`; `libnavigation-compose` on `coordination` and `libnavigation-compose-core`. `ev-driver-notification` does not depend on `ev`; it depends on `navigation` and `driver-notification` only.

`libnavigation-compose-foundation` depends on no in-scope library; it holds Compose primitives only.

Five optional libraries declare a native SDK directly: `coordination` (Nav SDK C++), `coordination-full-hd` (Navigation Native, Nav SDK C++ full HD), `datainputs`, `roadcam` and `libnavigation-compose-core` (Navigation Native). See `04_native_bridge.md`.

No optional-library dependency changed between `3.31.1` (tag `v0.31.1`) and `3.32.0-rc.1` (tag `v0.32.0-rc.1`).

---

## Key Design Rule

> **Optional libraries are additive, not layered.** An integrator can take any subset. Where one optional library depends on another (the EV, MapGPT, and Compose families), that is a deliberate product grouping, not an architectural layer; it does not imply the same dependency direction applies across unrelated optional libraries.
