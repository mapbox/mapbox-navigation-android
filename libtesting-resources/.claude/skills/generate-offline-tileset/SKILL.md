---
name: generate-offline-tileset
description: Generate an offline TileStore test asset (tileset_<name>.zip in libtesting-resources) with Maps (incl. HD roads), Navigation (SD routing) and Navigation HD tiles for an area given as a GeoJSON polygon or a W,S,E,N rectangle, using the mapbox-common-tilestore CLI. Load this skill whenever the user wants to create, extend, regenerate or debug offline tiles / a tile store / a tileset zip for an instrumentation test, prepare tiles for a location or route, add a Tileset enum entry, or asks why a test with onboard tiles does not switch to HD or does not render offline.
---

# Generate an offline tileset for instrumentation tests

Produces `src/main/assets/tileset_<name>.zip`: a zipped `tile_store/` directory that tests unpack
with `Context.unpackTiles(Tileset.X)` and then run fully offline, with an HD style, HD Lite / HD Full
and onboard routing.

All paths below are relative to `libtesting-resources/`. Scripts live in
`.claude/skills/generate-offline-tileset/scripts/` (stdlib Python + bash, no pip installs).

## Inputs to collect

1. **Area**: a GeoJSON file (Polygon / MultiPolygon / Feature / FeatureCollection, like
   geojson.io output) or a rectangle `W,S,E,N`. This is the **maps area**: everything the test
   drives through and the camera looks at. Keep it tight around the route, because maps and the
   z17+ single tiles grow with area. A few km² is typical; a whole city is too large.
2. **Name**: e.g. `ggb_tunnel_2026-09-27`. The convention is `<area>_<data date>`.
3. **Tile versions**:
   - `--nav-version`: SD navigation (routing) tiles, dataset `mapbox/driving-traffic`, e.g.
     `2026_09_20-06_15_51`
   - `--hd-version`: Navigation HD tiles, dataset `mapbox`, e.g. `2026-09-27-v2`; omit for an
     SD-only asset.

   When regenerating or extending an existing asset, keep its tile versions (listed in its
   `Tileset` KDoc and its nav config in `res/raw`): the route responses and expectations of the
   tests that use it were recorded against that data. For a new area, pick recent versions with
   `prepare_configs.py versions`. Old versions disappear from the server, so check they are still
   listed.
4. **Style** the test loads. The default is `mapbox://styles/mapbox-3dln/hd-roads-3dln-style`. It
   must be the exact URI the test passes to `loadStyle`.

`MAPBOX_ACCESS_TOKEN` must be set. It is used for tiles, styles and resources, so it needs access
to 3dln HD tiles and to the style's resources.

## Workflow

### 1. Build the CLI (once, ~25 min)

The tool lives in the monorepo at `projects/tools/tile_store` (it links gl-native). Build it
outside the repo:

```bash
B=/tmp/tools-build   # or any scratch dir
cmake -S <monorepo>/projects/tools -B $B -GNinja -DCMAKE_BUILD_TYPE=Release
ninja -C $B -j8 mapbox-common-tilestore
export TILESTORE_CLI=$B/bin/mapbox-common-tilestore
```

Released binaries also exist on the downloads API (`mapbox-common-tilestore/releases/other/<common
version>/mapbox-common-tilestore.tar.gz`), but may lag behind the TileStore schema of the SDK in
this branch. Prefer a local build.

### 2. Generate the configs

```bash
S=.claude/skills/generate-offline-tileset/scripts
python3 $S/prepare_configs.py configs --name <name> --bbox=W,S,E,N \
    --nav-version <nav> --hd-version <hd> --persistent-config
# or: --geojson area.geojson ; --style mapbox://styles/... ; --nav-margin-km 12
```

Use `--bbox=` with `=`, because the value starts with `-` for western longitudes.

This writes `tiles-downloading/configs/<name>/`:
- `maps.json`: the maps area, the style pack, and one maps tileset per dataset of the style.
- `includes/single_tiles.json`: every tile above z16 in the maps area, including empty ones.
- `navigation.json`: nav + nav-hd for the area bounding box plus 12 km on each side.

It also writes `src/main/res/raw/persistent_config_<name>.json`. Review any `WARNING` lines it
prints.

### 3. Download, verify, zip

```bash
bash $S/build_tileset.sh <name> [empty-work-dir]
```

The script loads every config into one `tile_store`, runs the integrity check, folds the SQLite
WAL, zips, copies the result to `src/main/assets/tileset_<name>.zip`, and prints the coverage.
`coverage.py <tile_store> --geojson out.geojson` gives the actual tile footprints for geojson.io.

Acceptance criteria:
- Every region reports `completed == required`.
- The check reports `missing: 0, bad size: 0, bad checksum: 0`.
- nav HD has a known set of cells around the area (empty ocean cells are fine).
- SD level 2 covers the area bbox plus 12 km.

An incomplete **style pack** is tolerated: the script reloads the tiles without `styles` and
reports the failed resources. They are typically style models the token has no access to (HTTP
403). The style still loads offline without them, but those models are not rendered. Rerun with a
token that has access if they matter.

### 4. Wire the asset into the tests

- The zips are large and go to Git LFS: `.gitattributes` already tracks
  `libtesting-resources/src/main/assets/*.zip`.
- Add a `Tileset` enum entry in `libtesting-navigation-core-utils/.../testing/utils/offline/Tileset.kt`:

  ```kotlin
  MyArea(
      "tileset_<name>",
      listOf(TileDataDomain.NAVIGATION, TileDataDomain.NAVIGATION_HD),
      "persistent_config_<name>",   // res/raw name, '-' replaced by '_'
  ),
  ```

  `unpackTiles` reads the versions from the `tile_store/navigation/dmapbox%2fdriving-traffic/v<ver>`
  and `tile_store/navigationhd/dmapbox/v<ver>` directory names. Other datasets need changes there.
- Commit the configs directory too, so the asset can be regenerated.

## Using the asset in a test

Configuring an instrumentation test to run fully offline from an asset (existing or new) is
covered by the `write-nav-integration-test` skill, see "Offline tiles" in its `utils-catalog.md`.

## Why the configs look like this (gotchas)

- **Navigation needs ±10.5 km around the ego.** navigation-native only considers an area usable
  (and will not switch to HD) once every tile within `requiredRadius` = 10.5 km is loaded. It
  loads out to `inMemoryRadius` = 11 km; see navigation-native `hd/cache/config.hpp` and
  `cache/config.hpp`.
  - Offline, a missing neighbouring tile never loads, and the test waits forever for HD Lite /
    HD Full.
  - Hence the separate `navigation.json` region with a 12 km margin.
  - Grids: HD uses 0.1° cells (x = (lon + 360) · 10, y = lat · 10); SD level 0/1/2 use 4°/1°/0.25°.
- **Maps tilesets must match the runtime lookup keys.** The CLI's `styles` only downloads the
  style pack (style, sprites, glyphs, models), not tiles. Tiles are looked up per dataset:
  - A composite source (`a,b,c`) is split into its datasets, with each constituent's zoom range.
  - batched-model sources use the TileJSON `variants` URL (e.g. `…-3dbuildings-v1-meshopt-v2-lod`).
  - The version is `""` unless the TileJSON has `language`/`worldview`
    (`&language=xx&worldview=YY`).
  - Levels follow the TileJSON `packs.scheme`, else the default `0[0-5] 6[6-10] 11[11-14] 12[15-16]`.

  `prepare_configs.py` derives all of this from the style.
- **No tilepacks above z16.** Levels such as HD roads' `13[17-18]` fail to download. Tiles above
  z16 are therefore bundled as single-tile `resources` keyed by the exact URL the map requests
  (`mapbox://tiles/<ids>/<z>/<x>/<y>.vector.pbf`).
  - Empty tiles (404) are included on purpose: they are stored as known-empty, so offline the map
    doesn't try the network for them.
  - Without these, HD Full (z19+) has no HD roads offline.
- **One geometry per CLI config.** Maps at z16 over the ±12 km navigation box would be hundreds of
  MB. That is why maps and navigation are separate configs, and therefore separate regions, in the
  same store.
- **`navigationhd` needs its URL template** under `tilestore-options` (3dln-tiles v2). The tileset
  domain is `nav-hd`.
- **Expired tiles are fine.** Navigation reads onboard tiles with `AcceptExpired`, and the regions
  are loaded with `accept-expired`. Existing assets have tiles that expired months ago.
- **Don't expect an ambient-cache capture to look the same.** An asset captured from an online
  app run (the SDK's ambient cache) looks different: it has more HD cells, and it may contain
  other styles or areas. A CLI-built asset only needs what is described here.
