#!/usr/bin/env python3

# Implements https://circleci.com/docs/2.0/api-job-trigger/

import os
import json
import requests
import sys

def TriggerPipeline(token, commit, job, run_benchmark):
    url = "https://circleci.com/api/v2/project/github/mapbox/mobile-metrics/pipeline"

    headers = {
        "Content-Type": "application/json",
        "Accept": "application/json",
    }

    data = {
        "parameters": {
          "run_android_navigation_benchmark": run_benchmark,
          "mapbox_slug": "mapbox/mapbox-navigation-android",
          "mapbox_hash": commit
        }
    }

    # Use this to test your mobile-metrics branches.
    # data["branch"]: "mobile-metrics-branch-name"

    response = requests.post(url, auth=(token, ""), headers=headers, json=data)

    if response.status_code != 201 and response.status_code != 200:
      print("Error triggering the CircleCI: %s." % response.json()["message"])
      sys.exit(1)
    else:
      response_dict = json.loads(response.text)
      print("Started %s: %s" % (job, response_dict))

def Main():
  token = os.getenv("MOBILE_METRICS_TOKEN")
  commit = os.getenv("CIRCLE_SHA1")
  
  if token is None:
    print("Error triggering because MOBILE_METRICS_TOKEN is not set")
    sys.exit(1)

  # Publish results that have been committed to the main branch.
  # Development runs can be found in CircleCI after manually triggered.
  publishResults = os.getenv("CIRCLE_BRANCH") == "master"
  if publishResults:
    TriggerPipeline(token, commit, "android-navigation-benchmark", True)
    TriggerPipeline(token, commit, "android-navigation-code-coverage", False)
    TriggerPipeline(token, commit, "android-navigation-binary-size", False)
  else:
    TriggerPipeline(token, commit, "android-navigation-benchmark", False)
    TriggerPipeline(token, commit, "android-navigation-code-coverage-ci", False)
    TriggerPipeline(token, commit, "android-navigation-binary-size-ci", False)

  return 0

if __name__ == "__main__":
    Main()
