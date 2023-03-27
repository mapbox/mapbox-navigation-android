import subprocess

import requests

import checkout_branch_utils

tags = requests.get('https://api.github.com/repos/mapbox/mapbox-navigation-android/git/refs/tags').json()
latest_tag = checkout_branch_utils.get_latest_tag(tags)
print('Latest no-patch release is ' + latest_tag)

snapshot_branch = checkout_branch_utils.get_snapshot_branch(latest_tag)
print('Snapshot branch is ' + snapshot_branch)
subprocess.run("git checkout " + snapshot_branch, shell=True, check=True)
