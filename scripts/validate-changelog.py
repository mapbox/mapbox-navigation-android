#!/usr/bin/python

import sys
import requests
import re

print("Validating that changelog entry is provided in the PR description...")

pr_number = sys.argv[1]
github_token = sys.argv[2]
url = "https://api.github.com/repos/mapbox/mapbox-navigation-android/pulls/" + pr_number
headers = { "accept": "application/vnd.github.v3+json", "authorization": "token " + github_token }
with requests.get(url, headers = headers) as r:
  response_json = r.json()
  pr_description = response_json["body"]
  pr_labels = response_json["labels"]

  skip_changelog = False
  if pr_labels is not None:
    for label in pr_labels:
      if label["name"] == "skip changelog":
        skip_changelog = True
        break

  matches = re.search(r'<changelog>(.+)</changelog>', pr_description, flags=re.S) is not None

  if skip_changelog and matches:
    raise Exception("Both `skip changelog` label and `<changelog></changelog>` closure present.")
  elif skip_changelog:
    print("`skip changelog` label present, exiting.")
  elif matches:
    print("Changelog entry validation successful.")
  else:
    raise Exception("Add a non-empty changelog entry in a `<changelog></changelog>` closure in the PR description or add a `skip changelog` label if not applicable.")
