# Layer Architecture Overview

**Diagram:** `01_layer_overview.svg`

## Purpose

This diagram shows how the Navigation SDK for Android divides into horizontal layers at release `3.32.0-rc.1` (tag `v0.32.0-rc.1`), where each layer depends only on the layers below it. This keeps high-level route and trip logic isolated from native-engine details, so the SDK stays testable and its public surface stays stable across native-engine changes.

---

## Layers (top to bottom)

### Calling App
**What it does:** The integrator's Android application. Not part of this SDK; shown as the outside actor that consumes it.

### UI Libraries
**What it does:** Map rendering, camera control, and turn-by-turn UI, built on top of Core.

Contains: `ui-base`, `ui-maps`, `ui-utils`, `ui-components`, `androidauto`. `androidauto`'s public API changed at this tag; see `02_module_map.md`.

### Core Libraries
**What it does:** Route state, native-engine bridging, and trip data. The layer every other in-scope library ultimately depends on.

Contains: `libnavigation-android`, `navigation`, `navigator`, `base`, `utils`, `tripdata`, `voice`, `copilot`, `metrics`, `notification`, `search`, `driver-notification`.

### Optional Libraries (side branch)
**What it does:** Product features an integrator opts into individually, such as EV routing, ADASIS, the coordination UI layer, MapGPT, road camera, weather, audio, and the compose wrappers. Each depends on Core directly; they do not form their own layer because no integrator is expected to take all of them.

### Native Bindings
**What it does:** The modules that declare a bound native SDK directly in their `build.gradle`. In the public tree these are `navigator` (the JNI bridge, no public API of its own) and `base`. In the internal tree, `coordination`, `coordination-full-hd`, `datainputs`, `roadcam` and `libnavigation-compose-core` also declare Navigation Native or Nav SDK C++ directly. See `04_native_bridge.md`.

### Bound Mapbox SDKs (external)
**What it does:** Mapbox native and platform libraries this SDK binds but does not design.

At Navigation SDK `3.32.0-rc.1`: Maps SDK `v11.32.0-rc.1`, Navigation Native `v324.32.0-rc.1`, Core Common `v24.32.0-rc.1`, Nav SDK C++ `v0.32.0-rc.1`, Search SDK `v2.32.0-rc.1`, Mapbox Java SDK `v7.10.1`.

---

## Key Design Rule

> **Dependencies flow downward only.** A lower layer never imports or calls into a higher layer. Optional libraries depend on Core; Core never depends on UI or Optional. Native SDKs are bound by the named set of modules in `04_native_bridge.md`; every other library reaches native code through them.
