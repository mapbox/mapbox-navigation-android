#!/usr/bin/env python3

# Implements https://circleci.com/docs/2.0/api-job-trigger/

import argparse
import os
import json
import requests
import sys

def TriggerJob(token, commit, job, publish):
    url = "https://circleci.com/api/v1.1/project/github/mapbox/mobile-metrics/tree/main" 

    headers = {
        "Content-Type": "application/json",
        "Accept": "application/json",
    }

    data = {
        "build_parameters": {
          "CIRCLE_JOB": job,
          "BENCHMARK_COMMIT": commit,
          "PUBLISH_RESULTS": publish,
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
  PARSER.add_argument('--publish', help="A flag indicating that metrics result should be published. By default published for main branch.", action="store_true")
  PARSER.add_argument('--no-publish', help="A flag indicating that metrics result should NOT be published. By default published for main branch.", action="store_false")
  PARSER.set_defaults(publish=os.getenv("CIRCLE_BRANCH") == "main")
  PARSER.add_argument('--performance-only', help="A flag indicating that metrics only performance metrics need to be run", action="store_true")
  PARSER.set_defaults(performance_only=False)
  ARGS = PARSER.parse_args()
  
  token = ARGS.token
  commits = ARGS.commits
  publishResults = ARGS.publish
  performanceOnly = ARGS.performance_only

  for commit in commits.split(','):
    TriggerJob(token, commit, "android-navigation-benchmark", publishResults)
    if not performanceOnly:
      if publishResults:
        TriggerJob(token, commit, "android-navigation-code-coverage", publishResults)
        TriggerJob(token, commit, "android-navigation-binary-size", publishResults)
      else:
        TriggerJob(token, commit, "android-navigation-code-coverage-ci", publishResults)
        TriggerJob(token, commit, "android-navigation-binary-size-ci", publishResults)

  return 0

if __name__ == "__main__":
    Main()
