#!/usr/bin/env python3

# Implements https://circleci.com/docs/2.0/api-job-trigger/

import argparse
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
          "run_android_navigation_benchmark": publish,
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
      print("Started run_android_navigation_benchmark: %s" % response_dict)

def TriggerJob(token, commit, job, publish):
    url = "https://circleci.com/api/v1.1/project/github/mapbox/mobile-metrics/tree/vv-publish-benchmark-results-from-job"

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
  PARSER = argparse.ArgumentParser(description='Script to run mobile-metrics')
  PARSER.add_argument('--commits', help='Comma separated SHAs of the commits of the Navigation SDK against which mobile-metrics will be run', required=True)
  PARSER.add_argument('-t', '--token', help='CircleCI token which is used to start metrics', required=True)
  PARSER.add_argument('--publish', help="A flag indicating that metrics result should be published", action="store_true",)
  PARSER.set_defaults(publish=False)

  ARGS = PARSER.parse_args()
  
  token = ARGS.token
  commits = ARGS.commits


  # Publish results that have been committed to the main branch.
  # Development runs can be found in CircleCI after manually triggered.
  #publishResults = os.getenv("CIRCLE_BRANCH") == "main"
  #TriggerWorkflow(token, commit, True)
  for commit in commits.split(','):
    #print(f'{token} {commit} {ARGS.publish}')
    TriggerJob(token, commit, "android-navigation-benchmark", ARGS.publish)
    #TriggerWorkflow(token, commit, ARGS.publish)
#
#
#   if publishResults:
#     TriggerJob(token, commit, "android-navigation-benchmark")
#     TriggerJob(token, commit, "android-navigation-code-coverage")
#     TriggerJob(token, commit, "android-navigation-binary-size")
#   else:
#     TriggerJob(token, commit, "android-navigation-code-coverage-ci")
#     TriggerJob(token, commit, "android-navigation-binary-size-ci")

  return 0

if __name__ == "__main__":
    Main()
