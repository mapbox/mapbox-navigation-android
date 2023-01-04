#!/usr/bin/python

import sys

import requests

import validate_changelog_utils

print("Validating that changelog entry is provided in the CHANGELOG.md...")

pr_number = sys.argv[1]
github_token = sys.argv[2]
api_url = "https://api.github.com/repos/mapbox/mapbox-navigation-android/pulls/" + str(pr_number)
headers = {"accept": "application/vnd.github.v3+json", "authorization": "token " + github_token}
files_path = "/files"
files_url = api_url + files_path

with requests.get(api_url, headers=headers) as pr_response:
    response_json = pr_response.json()

    skip_changelog = validate_changelog_utils.should_skip_changelog(response_json)

    if not skip_changelog:
        pr_diff_url = response_json["diff_url"]
        with requests.get(pr_diff_url, headers) as diff_response:
            diff = diff_response.text
            validate_changelog_utils.check_has_changelog_diff(diff)
            added_lines_by_file = validate_changelog_utils.extract_added_lines(diff)
            for lines in added_lines_by_file.values():
                validate_changelog_utils.check_for_duplications(added_lines_by_file)
            print("Changelog entry validation successful.")
    else:
        print("`skip changelog` label present, exiting.")
