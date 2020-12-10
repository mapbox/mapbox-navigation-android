#!/usr/bin/env bash

for file in $(find ./build/** -type f -name 'logo-styles.css');
do
  cat ./docs/mbx-styles.css > $file;
done
