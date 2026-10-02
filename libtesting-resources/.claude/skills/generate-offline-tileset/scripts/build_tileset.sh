#!/usr/bin/env bash
# Builds src/main/assets/tileset_<name>.zip from tiles-downloading/configs/<name>/*.json.
#
# Usage: build_tileset.sh <name> [work-dir]
#   TILESTORE_CLI   path to the mapbox-common-tilestore binary (required)
#   MAPBOX_ACCESS_TOKEN  token used for tiles, styles and resources (required)
#
# Every config of the directory is loaded into the same tile store (maps.json first, then
# navigation.json). If a style pack cannot be fully downloaded (typically: some style models are
# not accessible with the token), the config is loaded again without "styles" so that the tiles
# are still bundled; the partially loaded style pack stays in the store and is reported.
set -euo pipefail

name="${1:?usage: build_tileset.sh <name> [work-dir]}"
root="$(cd "$(dirname "$0")/../../../.." && pwd)"   # libtesting-resources
configs="$root/tiles-downloading/configs/$name"
work="${2:-$(mktemp -d "${TMPDIR:-/tmp}/tileset_${name}.XXXXXX")}"
cli="${TILESTORE_CLI:?set TILESTORE_CLI to the mapbox-common-tilestore binary}"
: "${MAPBOX_ACCESS_TOKEN:?set MAPBOX_ACCESS_TOKEN}"

[ -d "$configs" ] || { echo "No config directory $configs"; exit 1; }
[ ! -e "$work/tile_store" ] || { echo "$work/tile_store already exists, pass an empty work dir"; exit 1; }
mkdir -p "$work"
store="$work/tile_store"
echo "Work dir: $work"

for config in "$configs"/*.json; do
  log="$work/$(basename "$config" .json).log"
  echo "Loading $config"
  if "$cli" load --config-file="$config" --verbose --base-dir="$store" --concurrency=20 > "$log" 2>&1; then
    grep -E "Finished loading region|Coverage check" "$log" || true
    continue
  fi
  if grep -q "Could not download style pack" "$log"; then
    grep -E "Failed loading of [0-9]+ resources in style pack" "$log" | tail -1
    echo "Style pack incomplete, loading the tiles of $config without styles"
    tiles_only="$work/$(basename "$config" .json).tiles_only.json"
    python3 - "$config" "$tiles_only" <<'EOF'
import json, os, sys
src, dst = sys.argv[1:]
c = json.load(open(src))
c.pop("styles", None)
c["includes"] = [os.path.join(os.path.dirname(src), i) for i in c.get("includes", [])]
json.dump(c, open(dst, "w"), indent=1)
EOF
    "$cli" load --config-file="$tiles_only" --verbose --base-dir="$store" --concurrency=20 > "$log.tiles_only" 2>&1 \
      || { echo "Loading tiles failed, see $log.tiles_only"; tail -5 "$log.tiles_only"; exit 1; }
    grep -E "Finished loading region|Coverage check" "$log.tiles_only" || true
  else
    echo "Loading failed, see $log"; grep -vE '^\[ *[0-9.]+% after' "$log" | tail -10; exit 1
  fi
done

echo "Verifying"
# The CLI exits successfully even when the check finds broken tiles, so parse its summary.
summary=$("$cli" check --base-dir="$store" 2>&1 | grep "total:" | tail -1 || true)
echo "$summary"
case "$summary" in
  *"missing: 0, bad size: 0, bad checksum: 0"*) ;;
  *) echo "Integrity check failed, the archive is not created"; exit 1 ;;
esac
# Fold the WAL into metadata.db so that the archive holds a self-contained database.
sqlite3 "$store/metadata.db" "PRAGMA wal_checkpoint(TRUNCATE);" > /dev/null
echo "Regions (completed/required):"
sqlite3 "$store/metadata.db" "select '  ' || name || ': ' || completed_count || '/' || required_count from groups"
# Style packs (type 4) may stay incomplete, see the style pack fallback above. Tile regions may not.
incomplete=$(sqlite3 "$store/metadata.db" \
  "select group_concat(name, ', ') from groups where type != 4 and completed_count != required_count")
[ -z "$incomplete" ] || { echo "Incomplete regions: $incomplete, the archive is not created"; exit 1; }
failed=$(sqlite3 "$store/metadata.db" "select count(*) from resources where flags & 32")
[ "$failed" = "0" ] || echo "WARNING: $failed resources failed to download (see style pack above)"

archive="tileset_${name}.zip"
(cd "$work" && rm -f "$archive" && zip -q -r "$archive" tile_store)
cp "$work/$archive" "$root/src/main/assets/$archive"
ls -la "$root/src/main/assets/$archive"
python3 "$(dirname "$0")/coverage.py" "$store" --geojson "$work/coverage.geojson"
