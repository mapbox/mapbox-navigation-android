import os

import requests

token = os.getenv("MOBILE_METRICS_TOKEN")
commit = os.getenv("CIRCLE_SHA1")

url = "https://circleci.com/api/v2/project/github/mapbox/mobile-metrics/pipeline"

data = {
    "parameters": {
        "run_android_navigation_farm": True,
        "navigation_sdk_commit_hash": commit
    }
}

requests.post(url, auth=(token, ""), json=data)
