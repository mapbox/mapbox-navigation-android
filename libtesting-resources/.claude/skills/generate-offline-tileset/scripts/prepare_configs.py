#!/usr/bin/env python3
"""Generates mapbox-common-tilestore load configs for an offline test tileset.

Writes tiles-downloading/configs/<name>/{maps.json,navigation.json,includes/single_tiles.json}
and optionally the Nav SDK persistent config (res/raw/persistent_config_<name>.json).

Maps tilesets are derived from the style: every source of the style and of its imports is
resolved through its TileJSON into the exact TileStore datasets gl-native looks up at runtime.
Zoom levels above 16 (not available as tilepacks) are added as single-tile resources.

Stdlib only. Needs MAPBOX_ACCESS_TOKEN in the environment.

Examples:
  prepare_configs.py versions
  prepare_configs.py configs --name sf_downtown_2026-09-27 --bbox=-122.42,37.77,-122.39,37.80 \
      --nav-version 2026_09_20-06_15_51 --hd-version 2026-09-27-v2
  prepare_configs.py configs --name my_area --geojson area.geojson --nav-version ... --hd-version ...
"""

import argparse
import datetime
import json
import math
import os
import re
import sys
import urllib.parse
import urllib.request

API = "https://api.mapbox.com"
DEFAULT_STYLE = "mapbox://styles/mapbox-3dln/hd-roads-3dln-style"
# gl-native / tile store CLI default tilepack scheme: (index zoom, min zoom, max zoom).
DEFAULT_SCHEME = [(0, 0, 5), (6, 6, 10), (11, 11, 14), (12, 15, 16)]
# Tilepacks are not served above this zoom.
MAX_PACK_ZOOM = 16
# navigation-native needs every tile in a box of +-requiredRadius (10.5 km, inMemoryRadius 11 km)
# around the ego. Keep some margin on top.
DEFAULT_NAV_MARGIN_KM = 12.0
NAV_HD_URL_TEMPLATE = (
    "{mapbox_api_url}/3dln-tiles/v2/{dataset}/{version}/{x}-{y}"
    "?access_token={mapbox_access_token}&sku={mapbox_sku_token}"
)

# libtesting-resources directory (this file is <root>/.claude/skills/<skill>/scripts/).
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "..", ".."))


def token():
    t = os.environ.get("MAPBOX_ACCESS_TOKEN")
    if not t:
        sys.exit("MAPBOX_ACCESS_TOKEN is not set")
    return t


def get_json(url):
    sep = "&" if "?" in url else "?"
    req = urllib.request.Request(f"{url}{sep}access_token={token()}", headers={"User-Agent": "mapbox"})
    try:
        with urllib.request.urlopen(req) as r:
            return json.load(r)
    except urllib.error.HTTPError as e:
        # Never print the token.
        sys.exit(f"HTTP {e.code} for {url}")


# ---------------------------------------------------------------------------------------------
# Versions

def cmd_versions(_):
    nav = get_json(f"{API}/route-tiles/v1/versions")
    hd = get_json(f"{API}/3dln-tiles/v2/mapbox/versions")
    print("nav (mapbox/driving-traffic):", ", ".join(nav.get("availableVersions", [])[:10]))
    print("nav-hd (mapbox):", ", ".join(hd.get("availableVersions", [])[:10]))


# ---------------------------------------------------------------------------------------------
# Area

def load_area(args):
    if args.bbox:
        w, s, e, n = (float(v) for v in args.bbox.split(","))
        geometry = {"type": "Polygon", "coordinates": [[[w, n], [w, s], [e, s], [e, n], [w, n]]]}
    else:
        with open(args.geojson) as f:
            gj = json.load(f)
        geometries = []

        def collect(o):
            t = o.get("type")
            if t == "FeatureCollection":
                for feat in o["features"]:
                    collect(feat)
            elif t == "Feature":
                collect(o["geometry"])
            elif t in ("Polygon", "MultiPolygon"):
                geometries.append(o)
            else:
                sys.exit(f"Unsupported geometry type {t}: use Polygon / MultiPolygon")

        collect(gj)
        if len(geometries) == 1:
            geometry = geometries[0]
        else:
            polys = []
            for g in geometries:
                polys.extend([g["coordinates"]] if g["type"] == "Polygon" else g["coordinates"])
            geometry = {"type": "MultiPolygon", "coordinates": polys}
    return geometry


def bbox_of(geometry):
    rings = geometry["coordinates"] if geometry["type"] == "Polygon" else \
        [r for p in geometry["coordinates"] for r in p]
    pts = [pt for ring in rings for pt in ring]
    lons = [p[0] for p in pts]
    lats = [p[1] for p in pts]
    return min(lons), min(lats), max(lons), max(lats)


def expand_km(bbox, km):
    w, s, e, n = bbox
    dlat = km / 111.32
    dlon = km / (111.32 * math.cos(math.radians((s + n) / 2)))
    return w - dlon, s - dlat, e + dlon, n + dlat


def rect_feature_collection(w, s, e, n):
    r = lambda v: round(v, 4)
    return {"type": "FeatureCollection", "features": [{
        "type": "Feature", "properties": {},
        "geometry": {"type": "Polygon", "coordinates": [[
            [r(w), r(n)], [r(w), r(s)], [r(e), r(s)], [r(e), r(n)], [r(w), r(n)]]]}}]}


def tile_range(bbox, z):
    w, s, e, n = bbox
    count = 2 ** z

    def xy(lon, lat):
        x = int((lon + 180.0) / 360.0 * count)
        y = int((1.0 - math.asinh(math.tan(math.radians(lat))) / math.pi) / 2.0 * count)
        return min(max(x, 0), count - 1), min(max(y, 0), count - 1)

    x0, y0 = xy(w, n)
    x1, y1 = xy(e, s)
    return x0, x1, y0, y1


# ---------------------------------------------------------------------------------------------
# Style -> maps tilesets

def style_url(uri):
    m = re.match(r"mapbox://styles/([^/]+)/([^/?]+)", uri)
    if not m:
        sys.exit(f"Not a mapbox:// style URI: {uri}")
    return f"{API}/styles/v1/{m.group(1)}/{m.group(2)}"


def collect_sources(style, found, warnings, visited):
    for source_id, source in style.get("sources", {}).items():
        url = source.get("url")
        if url and url.startswith("mapbox://"):
            found.setdefault(url, source.get("type"))
        elif source.get("type") in ("geojson", "image", "video"):
            continue
        else:
            warnings.append(f"source '{source_id}' has no mapbox:// url, not bundled")
    for imp in style.get("imports", []):
        if "data" in imp:
            collect_sources(imp["data"], found, warnings, visited)
        elif imp.get("url") and imp["url"] not in visited:
            visited.add(imp["url"])
            collect_sources(get_json(style_url(imp["url"])), found, warnings, visited)


def select_variant(tilejson):
    """Mirrors gl-native parseTileJSONVariants: prefer meshopt-v2+lod, then meshopt."""
    chosen = None
    for variant in tilejson.get("variants", []):
        caps = variant.get("capabilities", [])
        if caps == ["meshopt-v2+lod"]:
            chosen = variant
            break
        if caps == ["meshopt"]:
            chosen = variant
    if chosen:
        tilejson = dict(tilejson)
        tilejson.update({k: v for k, v in chosen.items() if k != "capabilities"})
    return tilejson


def query_without_token(tile_url):
    """Returns the query of tile_url without access_token, like gl-native's canonicalizeTileURL."""
    if "?" not in tile_url:
        return ""
    params = [p for p in tile_url.split("?", 1)[1].split("&") if p and not p.startswith("access_token=")]
    return "?" + "&".join(params) if params else ""


def canonical_tile_url(tile_url):
    """Returns the mapbox:// URL the Maps SDK uses as ambient cache key, and its dataset ids."""
    path = tile_url.split("?")[0]
    for prefix, canonical in (
        ("mapbox://tiles/", "mapbox://tiles/"),
        ("mapbox://3dtiles/", "mapbox://3dtiles/"),
        ("/v4/", "mapbox://tiles/"),
        ("/3dtiles/v1/", "mapbox://3dtiles/"),
        ("/rasterarrays/v1/", None),
        ("/raster/v1/", None),
    ):
        i = path.find(prefix)
        if i >= 0:
            rest = path[i + len(prefix):]
            ids = rest.split("/")[0]
            url = canonical + rest + query_without_token(tile_url) if canonical else None
            return url, ids.split(",")
    return None, []


def maps_tilesets(style_uri, area_bbox, max_single_tiles, warnings):
    sources = {}
    collect_sources(get_json(style_url(style_uri)), sources, warnings, {style_uri})

    tilesets = {}
    single_tiles = []
    for source_url, source_type in sorted(sources.items()):
        ids = source_url[len("mapbox://"):]
        tj = select_variant(get_json(f"{API}/v4/{urllib.parse.quote(ids, safe=',.-_')}.json?secure"))
        tiles = tj.get("tiles") or []
        if not tiles:
            warnings.append(f"{source_url}: TileJSON has no tiles, skipped")
            continue
        tile_url, datasets = canonical_tile_url(tiles[0])
        src_min, src_max = tj.get("minzoom", 0), tj.get("maxzoom", 22)
        zooms = {d: (src_min, src_max) for d in datasets}
        for c in tj.get("constituents", []):
            cid = c.get("source") or c.get("url", "").replace("mapbox://", "")
            if cid in zooms:
                zooms[cid] = (c.get("minzoom", src_min), c.get("maxzoom", src_max))
        scheme = [(l["indexzoom"], l["minzoom"], l["maxzoom"]) for l in (tj.get("packs") or {}).get("scheme", [])] \
            or DEFAULT_SCHEME
        for d in datasets:
            zmin, zmax = zooms[d]
            levels = [list(l) for l in scheme if l[2] <= MAX_PACK_ZOOM and l[2] >= zmin and l[1] <= zmax]
            parts = []
            if isinstance(tj.get("language"), dict) and d in tj["language"]:
                parts.append(f"&language={tj['language'][d]}")
            if isinstance(tj.get("worldview"), dict) and d in tj["worldview"]:
                parts.append(f"&worldview={tj['worldview'][d]}")
            entry = {"domain": "maps", "name": d, "levels": levels}
            if parts:
                entry["version"] = "".join(parts)
            if levels:
                tilesets[d] = entry
        if source_type == "raster":
            warnings.append(f"{source_url}: raster source, check tileSize/pixel_ratio of its tilepack version")

        # Zooms above the tilepack maximum are looked up as single tiles (ambient cache).
        if src_max > MAX_PACK_ZOOM:
            if not tile_url:
                warnings.append(f"{source_url}: z{MAX_PACK_ZOOM + 1}-{src_max} tiles not bundled "
                                f"(unsupported tile URL for single tiles)")
                continue
            for z in range(max(MAX_PACK_ZOOM + 1, src_min), src_max + 1):
                x0, x1, y0, y1 = tile_range(area_bbox, z)
                for x in range(x0, x1 + 1):
                    for y in range(y0, y1 + 1):
                        url = tile_url.replace("{z}", str(z)).replace("{x}", str(x)).replace("{y}", str(y))
                        single_tiles.append({"domain": "maps", "url": url})
    if len(single_tiles) > max_single_tiles:
        sys.exit(f"{len(single_tiles)} single tiles above z{MAX_PACK_ZOOM} for this area (limit "
                 f"{max_single_tiles}). Use a smaller maps area or raise --max-single-tiles.")
    return list(tilesets.values()), single_tiles


# ---------------------------------------------------------------------------------------------

def write_json(path, obj, compact_list_key=None):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        if compact_list_key:
            f.write('{\n  "%s": [\n' % compact_list_key)
            f.write(",\n".join("    " + json.dumps(i) for i in obj[compact_list_key]))
            f.write("\n  ]\n}\n")
        else:
            json.dump(obj, f, indent=2)
            f.write("\n")


def cmd_configs(args):
    warnings = []
    geometry = load_area(args)
    area = bbox_of(geometry)
    nav_bbox = expand_km(area, args.nav_margin_km)

    tilesets, single_tiles = maps_tilesets(args.style, area, args.max_single_tiles, warnings)
    out = os.path.join(ROOT, "tiles-downloading", "configs", args.name)

    maps = {
        "includes": ["includes/single_tiles.json"],
        "name": f"{args.name}-maps",
        "estimate-only": False,
        "accept-expired": True,
        "styles": [args.style],
        "tilesets": tilesets,
        "geojson": {"type": "FeatureCollection",
                    "features": [{"type": "Feature", "properties": {}, "geometry": geometry}]},
    }
    nav_tilesets = [{"domain": "nav", "name": "mapbox/driving-traffic", "version": args.nav_version}]
    nav = {
        "name": f"{args.name}-navigation",
        "estimate-only": False,
        "accept-expired": True,
        "tilesets": nav_tilesets,
        "geojson": rect_feature_collection(*nav_bbox),
    }
    if args.hd_version:
        nav_tilesets.append({"domain": "nav-hd", "name": "mapbox", "version": args.hd_version})
        nav["tilestore-options"] = {"navigationhd": {
            "mapbox-api-url": API, "tile-url-template": NAV_HD_URL_TEMPLATE}}

    write_json(os.path.join(out, "maps.json"), maps)
    write_json(os.path.join(out, "includes", "single_tiles.json"), {"resources": single_tiles}, "resources")
    write_json(os.path.join(out, "navigation.json"), nav)

    if args.persistent_config:
        today = datetime.datetime.utcnow().strftime("%Y-%m-%dT00:00")
        cfg = {"tiles": {"latestVersions": [
            {"dataSet": "mapbox/driving-traffic", "version": args.nav_version, "discoveryTime": today}]}}
        if args.hd_version:
            cfg["tilesHd"] = {"latestVersions": [
                {"dataSet": "mapbox", "version": args.hd_version, "discoveryTime": today}]}
        path = os.path.join(ROOT, "src", "main", "res", "raw", f"persistent_config_{args.name.replace('-', '_')}.json")
        with open(path, "w") as f:
            f.write(json.dumps(cfg, separators=(",", ":")))
        print(f"wrote {path}")

    print(f"wrote {out}/maps.json ({len(tilesets)} maps tilesets)")
    print(f"wrote {out}/includes/single_tiles.json ({len(single_tiles)} single tiles above z{MAX_PACK_ZOOM})")
    print(f"wrote {out}/navigation.json (area bbox + {args.nav_margin_km} km: "
          + ", ".join(f"{v:.4f}" for v in nav_bbox) + ")")
    for w in warnings:
        print(f"WARNING: {w}")


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="cmd", required=True)
    sub.add_parser("versions", help="list available nav and nav-hd tile versions").set_defaults(func=cmd_versions)
    c = sub.add_parser("configs", help="write load configs for an area")
    area = c.add_mutually_exclusive_group(required=True)
    area.add_argument("--bbox", help="W,S,E,N of the maps area; use the = form: --bbox=-122.5,37.7,-122.4,37.8")
    area.add_argument("--geojson", help="GeoJSON file with the maps area (Polygon/MultiPolygon/Feature/FC)")
    c.add_argument("--name", required=True, help="tileset name, the asset becomes tileset_<name>.zip")
    c.add_argument("--nav-version", required=True,
                   help="SD navigation (routing) tiles version, dataset mapbox/driving-traffic, "
                        "e.g. 2026_09_20-06_15_51")
    c.add_argument("--hd-version", help="nav-hd 'mapbox' version, e.g. 2026-09-27-v2 (omit for SD only)")
    c.add_argument("--style", default=DEFAULT_STYLE, help=f"style the test loads (default {DEFAULT_STYLE})")
    c.add_argument("--nav-margin-km", type=float, default=DEFAULT_NAV_MARGIN_KM)
    c.add_argument("--max-single-tiles", type=int, default=20000)
    c.add_argument("--persistent-config", action="store_true",
                   help="also write src/main/res/raw/persistent_config_<name>.json")
    c.set_defaults(func=cmd_configs)
    args = p.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
