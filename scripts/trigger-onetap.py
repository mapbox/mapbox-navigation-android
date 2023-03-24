#!/usr/bin/env python3

import json
import os
import subprocess
import sys

import requests

token = os.getenv("CIRCLE_TOKEN")

last_release = str(subprocess.run("git describe --tag --match 'v*' --abbrev=0", shell=True, capture_output=True,
                                  text=True).stdout).strip()
print("Last release " + last_release)

release_main_part = last_release.partition('-')[0]
snapshot_name = release_main_part + "-WEEKLY-SNAPSHOT"
print("Snapshot name " + snapshot_name)

if token is None:
    print("Error triggering because CIRCLE_TOKEN is not set")
    sys.exit(1)

url = "https://circleci.com/api/v2/project/github/mapbox/1tap-android/pipeline"

headers = {
    "Content-Type": "application/json",
    "Accept": "application/json",
}

data = {
    "parameters": {
        "run_weekly_snapshot": True,
        "navigation_sdk_snapshot_version": snapshot_name
    }
}

response = requests.post(url, auth=(token, ""), headers=headers, json=data)

if response.status_code != 201 and response.status_code != 200:
    print("Error triggering the CircleCI: %s." % response.json()["message"])
    sys.exit(1)
else:
    response_dict = json.loads(response.text)
    print("Started run_weekly_snapshot: %s" % response_dict)
