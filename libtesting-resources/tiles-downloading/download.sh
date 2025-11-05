for file in configs/*; do
  if [ -f "$file" ]; then
    filename=$(basename -- "$file")

    echo "Downloading $filename"
    ../../../tools/mapbox-common-tilestore load --config-file="$file" --verbose --base-dir=tile_store --concurrency=20

    echo "Verifying the $filename"
    ../../../tools/mapbox-common-tilestore check --verbose --base-dir=tile_store

    archive="tileset_${filename%.*}.zip"
    zip "$archive" tile_store -r
    mv "$archive" "../src/main/assets/$archive"
    rm -rf tile_store
  fi
done
