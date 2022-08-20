#!/usr/bin/python

import requests
import sys
import base64
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
            print("[ddlog] diff: " + diff)
            validate_changelog_utils.check_has_changelog_diff(diff)
            added_lines = validate_changelog_utils.extract_added_lines(diff)
            validate_changelog_utils.check_contains_pr_link(added_lines)
            with requests.get(files_url, headers) as files_response:
                contents_url = validate_changelog_utils.parse_contents_url(files_response.json())
                with requests.get(contents_url, headers) as contents_response:
                    content = base64.b64decode(contents_response.json()["content"]).decode("utf-8")
                    validate_changelog_utils.check_version_section(content, added_lines)
            print("Changelog entry validation successful.")
    else:
        print("`skip changelog` label present, exiting.")
