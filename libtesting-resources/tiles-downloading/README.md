### Downloading tileset_helsinki.zip (used in coordination tests)

1. Install AWS CLI (if not installed):
```
brew install awscli
```

2. mbx env:
```
mbx env
```

3. Download the file (the target path is specified if you are working from navigation/projects/mapbox-navigation-internal directory):
```
aws s3 cp "s3://mapbox-navigation-android/testing/tileset_helsinki.zip" mapbox-navigation-android/libtesting-resources/src/main/assets
```

This file path is added to gitignore, so no need to commit it or remove from unstaged changes: you can keep the file locally and re-download only when it has changed. 