#!/usr/bin/env python3

# Implements https://circleci.com/docs/2.0/api-job-trigger/

import json
import os
import sys

import requests


def trigger_workflow(token, commit, publish):
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


def trigger_job(token, commit, job):
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


def main():
    token = os.getenv("MOBILE_METRICS_TOKEN")
    commit = os.getenv("CIRCLE_SHA1")

    if token is None:
        print("Error triggering because MOBILE_METRICS_TOKEN is not set")
        sys.exit(1)

    # Publish results that have been committed to the main branch.
    # Development runs can be found in CircleCI after manually triggered.
    publish_results = os.getenv("CIRCLE_BRANCH") == "main"

    trigger_workflow(token, commit, publish_results)

    if not publish_results:
        trigger_job(token, commit, "android-navigation-code-coverage-ci")
        trigger_job(token, commit, "android-navigation-binary-size-ci")

    return 0


if __name__ == "__main__":
    main()
