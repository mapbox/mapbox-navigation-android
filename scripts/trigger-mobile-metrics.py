#!/usr/bin/env python3

# Implements https://circleci.com/docs/2.0/api-job-trigger/

import os
import json
import requests
import sys

def TriggerWorkflow(token, commit, publish):
    url = "https://circleci.com/api/v2/project/github/mapbox/mobile-metrics/pipeline"

    headers = {
        "Content-Type": "application/json",
        "Accept": "application/json",
    }

    data = {
        "parameters": {
          "run_android_navigation_benchmark": True,
          "publish_android_navigation_benchmark": publish,
          "mapbox_slug": "mapbox/mapbox-navigation-android",
          "navigation_sdk_commit_hash": commit,
          "android_navigation_directory": "android-navigation"
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
      print("Started run_android_navigation_benchmark: %s" % response_dict)

def TriggerJob(token, commit, job):
    url = "https://circleci.com/api/v1.1/project/github/mapbox/mobile-metrics/tree/master"

    headers = {
        "Content-Type": "application/json",
        "Accept": "application/json",
    }

    data = {
        "build_parameters": {
          "CIRCLE_JOB": job,
          "BENCHMARK_COMMIT": commit
        }
    }

    response = requests.post(url, auth=(token, ""), headers=headers, json=data)

    if response.status_code != 201 and response.status_code != 200:
      print("Error triggering the CircleCI: %s." % response.json()["message"])
      sys.exit(1)
    else:
      response_dict = json.loads(response.text)
      build_url = response_dict['build_url']
      print("Started %s: %s" % (job, build_url))

def Main():
  token = os.getenv("MOBILE_METRICS_TOKEN")
  commit = os.getenv("CIRCLE_SHA1")
  
  if token is None:
    print("Error triggering because MOBILE_METRICS_TOKEN is not set")
    sys.exit(1)

  # Publish results that have been committed to the main branch.
  # Development runs can be found in CircleCI after manually triggered.
  publishResults = os.getenv("CIRCLE_BRANCH") == "main"
  TriggerWorkflow(token, commit, publishResults)

  if publishResults:
    TriggerJob(token, commit, "android-navigation-code-coverage")
    TriggerJob(token, commit, "android-navigation-binary-size")
  else:
    TriggerJob(token, commit, "android-navigation-code-coverage-ci")
    TriggerJob(token, commit, "android-navigation-binary-size-ci")

  return 0

if __name__ == "__main__":
    Main()
