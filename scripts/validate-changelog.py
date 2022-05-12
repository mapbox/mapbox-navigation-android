#!/usr/bin/python

import re
import requests
import sys

print("Validating that changelog entry is provided in the CHANGELOG.md...")

pr_number = sys.argv[1]
github_token = sys.argv[2]
api_url = "https://api.github.com/repos/mapbox/mapbox-navigation-android/pulls/" + str(pr_number)
pr_link_regex = "\[#\d+]\(https:\/\/github\.com\/mapbox\/mapbox-navigation-android\/pull\/\d+\)"
headers = {"accept": "application/vnd.github.v3+json", "authorization": "token " + github_token}
changelog_diff_regex = "diff --git a(.*)\/CHANGELOG.md b(.*)\/CHANGELOG.md"
any_diff_substring = "diff --git"

with requests.get(api_url, headers=headers) as pr_response:
    response_json = pr_response.json()
    pr_labels = response_json["labels"]

    skip_changelog = False
    if pr_labels is not None:
        for label in pr_labels:
            if label["name"] == "skip changelog":
                skip_changelog = True
                break

    if not skip_changelog:
        pr_diff_url = response_json["diff_url"]
        with requests.get(pr_diff_url, headers) as diff_response:
            diff = diff_response.text
            changelog_diff_matches = re.search(changelog_diff_regex, diff)
            if not changelog_diff_matches:
                raise Exception("Add a non-empty changelog entry in a CHANGELOG.md or add a `skip changelog` label if not applicable.")
            else:
                last_reachable_index = diff.find(any_diff_substring, changelog_diff_matches.end())
                if last_reachable_index == -1:
                    last_reachable_index = len(diff)
                diff_searchable = diff[changelog_diff_matches.end():last_reachable_index]
                pr_link_matches = re.search(pr_link_regex, diff_searchable)
                if not pr_link_matches:
                    raise Exception("The changelog entry should contain a link to the original PR that matches `" + pr_link_regex + "`")
                print("Changelog entry validation successful.")
    else:
        print("`skip changelog` label present, exiting.")
