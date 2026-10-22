# Module Map

**Diagram:** `02_module_map.svg`

## Purpose

This diagram lists every in-scope Gradle library module at release `3.32.0-rc.1` (tag `v0.32.0-rc.1`), grouped into Core, UI, and Optional, so a reader can see the SDK's full shape at a glance without opening either `settings.gradle`. Core and UI libraries live in the public tree `mapbox-navigation-android/` and ship as `com.mapbox.navigationcore:<module>` artifacts. Optional libraries live in the internal tree at the repository root and depend on the public tree through those artifact coordinates.

---

## Core Libraries

`libnavigation-android` (aggregate public artifact), `navigation` (core logic, `MapboxNavigation`), `navigator` (JNI bridge), `base` (base interfaces), `utils` (shared utilities), `tripdata` (route progress and trip data), `voice` (voice instructions), `copilot` (session recording), `metrics` (telemetry), `notification` (trip notifications), `search` (search integration), `driver-notification` (driver-facing alerts).

## UI Libraries

`ui-base`, `ui-maps` (map rendering and camera), `ui-utils`, `ui-components` (reusable widgets), `androidauto` (Android Auto screens; public API changed at this tag, see below).

## Optional Libraries

`coordination` and `coordination-full-hd` (Redux-style UI coordination layer), `adasis` (ADAS integration), `ev` / `ev-ui` / `ev-rangemap` / `ev-driver-notification` (electric-vehicle family), `roadcam` / `roadcam-ui` (road camera detection), `weather`, `audio`, `core-mapgpt` / `ui-mapgpt` / `applemusic-mapgpt` (MapGPT family), `datainputs` (external vehicle-signal feed), `noa` (natural-language nav assistant), `libnavigation-custom-route` (custom route providers), `libnavigation-compose` / `-compose-core` / `-compose-foundation` (Compose wrappers).

## Excluded from this record

Application, demo, test-harness, instrumentation, and benchmark modules are out of scope for this CI type and are not diagrammed: `app-qa-androidauto`, `qa-test-app`, `screenshot-tests`, `billing-tests`, `instrumentation-tests(-internal)`, `app-tests-wrapper`, every `libtesting-*` module, `microbenchmark-internal`, `app-compose-demo`, `mapgpt-showcase-app`, `full-hd-replayer`. `libnavui-resources` is a resources-only module, not a functional library, and is excluded from this record on that basis.

## What changed at this tag

`androidauto`'s `api/current.txt` changed between `3.31.1` (tag `v0.31.1`) and `3.32.0-rc.1` (tag `v0.32.0-rc.1`). See the release context summary for detail; no other library's public API or module set changed.

---

## Key Design Rule

> **Module independence.** UI and Optional libraries depend on Core; Core never depends on UI or Optional. Optional libraries are independent of each other except where `03_optional_libraries.md` names a dependency (for example, `ev-ui` depends on `ev`, `ui-mapgpt` depends on `core-mapgpt`, `adasis` depends on `weather`).
