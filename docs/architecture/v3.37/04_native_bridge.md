# Native Bindings

**Diagram:** `04_native_bridge.svg`

## Purpose

This diagram names every in-scope module whose `build.gradle` declares a bound native SDK directly at release line `3.37`, and which SDK each one declares. A reader uses it to know which modules a native ABI or version change touches.

---

## Modules with a direct native binding

Public tree (`mapbox-navigation-android/`):

- `navigator`: the JNI bridge to Navigation Native (`324.37.0`). Its `api/current.txt` holds the signature header only; it exposes no tracked public class or interface. Integrators never call it directly. `navigation` depends on it through `project(':navigator')`.
- `base`: declares Navigation Native directly alongside Core Common and the Mapbox Java SDK models.

Internal tree (repository root):

- `coordination`: declares Nav SDK C++ (`0.37.0`).
- `coordination-full-hd`: declares Navigation Native, Nav SDK C++ and the Nav SDK C++ full HD artifact (`0.37.0`).
- `datainputs`: declares Navigation Native.
- `roadcam`: declares Navigation Native.
- `libnavigation-compose-core`: declares Navigation Native.

Navigation Native and Nav SDK C++ both depend on Core Common (`24.37.0`) for networking, tile store and location.

`ui-maps` references Navigation Native in its test configuration only, and `navigation` reads the Navigation Native version string into its `BuildConfig` without declaring the artifact. Neither counts as a binding here.

No native-binding declaration changed since `3.36.0-rc.1`.

---

## Key Design Rule

> **A named set of native binders.** These seven modules are the only in-scope libraries that declare a native SDK. Every other Core, UI and Optional library reaches native code through them. A native ABI or version change is scoped to these seven `build.gradle` files and their internals.
