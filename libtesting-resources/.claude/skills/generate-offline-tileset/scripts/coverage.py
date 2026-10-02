#!/usr/bin/env python3
"""Prints what a tile store actually contains and where, optionally as GeoJSON.

Usage: coverage.py <tile_store dir> [--geojson out.geojson]

Footprints are whole tiles, so they are larger than the load config polygons.
"""

import argparse
import collections
import json
import math
import os
import sqlite3

# TileDataDomain values in metadata.db
MAPS, NAV, NAV_HD = 0, 1, 4
# Navigation tile levels are fixed lon/lat grids (degrees per tile).
NAV_LEVEL_DEGREES = {0: 4.0, 1: 1.0, 2: 0.25}
NAV_HD_DEGREES = 0.1
EMPTY_TILE_BYTES = 28


def web_mercator_bbox(z, x, y):
    n = 2 ** z
    lon = lambda v: v / n * 360 - 180
    lat = lambda v: math.degrees(math.atan(math.sinh(math.pi * (1 - 2 * v / n))))
    return lon(x), lat(y + 1), lon(x + 1), lat(y)


def union(boxes):
    boxes = list(boxes)
    return (min(b[0] for b in boxes), min(b[1] for b in boxes), max(b[2] for b in boxes), max(b[3] for b in boxes))


def feature(name, bbox, **props):
    w, s, e, n = (round(v, 5) for v in bbox)
    return {"type": "Feature", "properties": {"name": name, **props},
            "geometry": {"type": "Polygon", "coordinates": [[[w, n], [e, n], [e, s], [w, s], [w, n]]]}}


def main():
    p = argparse.ArgumentParser()
    p.add_argument("store")
    p.add_argument("--geojson")
    args = p.parse_args()
    db = sqlite3.connect(os.path.join(args.store, "metadata.db"))
    features = []

    print("Regions:")
    for name, req, done in db.execute("select name, required_count, completed_count from groups"):
        print(f"  {name}: {done}/{req}")

    # Maps tilepacks. level = index << 8 | (min - index) << 4 | (max - min)
    packs = collections.defaultdict(lambda: collections.defaultdict(set))
    for ds, level, x, y in db.execute(
            "select v.dataset, t.level, t.x, t.y from tiles t join variants v on v.id = t.variant_id "
            "where v.domain = ?", (MAPS,)):
        idx = level >> 8
        zmin = idx + ((level >> 4) & 0xF)
        zmax = zmin + (level & 0xF)
        packs[(idx, zmin, zmax)][(x, y)].add(ds)
    print("Maps tilepacks:")
    for (idx, zmin, zmax), tiles in sorted(packs.items()):
        bbox = union(web_mercator_bbox(idx, x, y) for x, y in tiles)
        datasets = sorted(set().union(*tiles.values()))
        print(f"  z{zmin}-{zmax}: {len(tiles)} pack(s), {len(datasets)} datasets, "
              f"bbox {', '.join(f'{v:.5f}' for v in bbox)}")
        if idx > 0:
            features.append(feature(f"maps z{zmin}-{zmax} tilepacks", bbox, datasets=datasets))

    # Maps single tiles (ambient-style resources), grouped by source and zoom.
    single = collections.defaultdict(list)
    for url, flags, size in db.execute("select url, flags, bytes from resources where url like 'mapbox://%tiles/%'"):
        parts = url.split("?")[0].split("/")
        try:
            z, x, y = int(parts[-3]), int(parts[-2]), int(parts[-1].split(".")[0])
        except ValueError:
            continue
        single[("/".join(parts[2:-3]), z)].append((x, y, size == EMPTY_TILE_BYTES))
    if single:
        print("Maps single tiles:")
    for (source, z), tiles in sorted(single.items()):
        bbox = union(web_mercator_bbox(z, x, y) for x, y, _ in tiles)
        empty = sum(1 for *_, e in tiles if e)
        print(f"  {source} z{z}: {len(tiles)} tiles ({empty} empty), bbox {', '.join(f'{v:.5f}' for v in bbox)}")
        features.append(feature(f"maps single tiles z{z}", bbox, source=source, tiles=len(tiles), empty=empty))

    # Navigation.
    for domain, label in ((NAV, "nav SD"), (NAV_HD, "nav HD")):
        rows = list(db.execute(
            "select t.level, t.x, t.y, t.bytes from tiles t join variants v on v.id = t.variant_id "
            "where v.domain = ?", (domain,)))
        if not rows:
            continue
        print(f"{label} tiles:")
        by_level = collections.defaultdict(list)
        for level, x, y, size in rows:
            deg = NAV_HD_DEGREES if domain == NAV_HD else NAV_LEVEL_DEGREES.get(level)
            if deg is None:
                continue
            by_level[level].append(((x * deg - 180 if domain == NAV else x * deg - 360,
                                     y * deg - 90 if domain == NAV else y * deg), deg, size))
        for level, tiles in sorted(by_level.items()):
            boxes = [(w, s, w + d, s + d) for (w, s), d, _ in tiles]
            empty = sum(1 for *_, size in tiles if size == EMPTY_TILE_BYTES)
            bbox = union(boxes)
            print(f"  level {level}: {len(tiles)} tiles ({empty} empty), bbox {', '.join(f'{v:.4f}' for v in bbox)}")
            features.append(feature(f"{label} level {level}", bbox, tiles=len(tiles), empty=empty))

    failed = [u for (u,) in db.execute("select url from resources where flags & 32")]
    if failed:
        print(f"Failed resources: {len(failed)}, e.g. {failed[0].split('?')[0]}")

    if args.geojson:
        with open(args.geojson, "w") as f:
            json.dump({"type": "FeatureCollection", "features": features}, f, indent=1)
        print(f"Coverage GeoJSON: {args.geojson}")


if __name__ == "__main__":
    main()
