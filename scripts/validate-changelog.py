#!/usr/bin/python

import re
import requests
import sys
import base64

print("Validating that changelog entry is provided in the CHANGELOG.md...")

pr_number = sys.argv[1]
github_token = sys.argv[2]
api_url = "https://api.github.com/repos/mapbox/mapbox-navigation-android/pulls/" + str(pr_number)
pr_link_regex = "\[#\d+]\(https:\/\/github\.com\/mapbox\/mapbox-navigation-android\/pull\/\d+\)"
headers = {"accept": "application/vnd.github.v3+json", "authorization": "token " + github_token}
changelog_diff_regex = "diff --git a(.*)\/CHANGELOG.md b(.*)\/CHANGELOG.md"
any_diff_substring = "diff --git"
diff_file_start_regex = "\\n\@\@(.*)\@\@"
changelog_filename = "CHANGELOG.md"
files_path = "/files"

def is_line_added(line):
    return line.startswith('+')

def remove_plus(line):
    return line[1:]

def group_by_versions(lines):
    groups = {}
    group = []
    group_name = ""
    for line in lines:
        if line.startswith("##") and len(line) > 2 and line[2] != '#':
            if (len(group) > 0):
                if len(group_name.strip()) > 0:
                    groups[group_name] = group
                group = []
            group_name = line
        elif len(line.strip()) > 0:
            group.append(line)
    if len(group) > 0 and len(group_name.strip()) > 0:
        groups[group_name] = group
    return groups

def extract_unreleased_group(versions):
    for version in versions.keys():
        if 'Unreleased' in version:
            return versions[version]
    raise Exception("No 'Unreleased' section in CHANGELOG")

def extract_stable_versions(versions):
    str_before_version = "Mapbox Navigation SDK "
    str_after_version = " "
    stable_versions = {}
    pattern = re.compile("[0-9]+\.[0-9]+\.[0-9]+")
    for version in versions.keys():
        beginIndex = version.find(str_before_version)
        if beginIndex == -1:
            continue
        version_name = version[(beginIndex + len(str_before_version)):]
        endIndex = version_name.find(str_after_version)
        if endIndex == -1:
            continue
        version_name = version_name[:endIndex]
        if pattern.fullmatch(version_name) != None:
            stable_versions[version_name] = versions[version]
    return stable_versions

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
                diff_starting_at_changelog = diff[changelog_diff_matches.end():]
                first_changelog_diff_index = re.search(diff_file_start_regex, diff_starting_at_changelog).end()
                if first_changelog_diff_index == -1:
                    first_changelog_diff_index = 0
                last_reachable_index = diff_starting_at_changelog.find(any_diff_substring, first_changelog_diff_index)
                if last_reachable_index == -1:
                    last_reachable_index = len(diff_starting_at_changelog)
                diff_searchable = diff_starting_at_changelog[first_changelog_diff_index:last_reachable_index]

                diff_lines = diff_searchable.split('\n')
                added_lines = list(map(remove_plus, list(filter(is_line_added, diff_lines))))

                files_url = api_url + files_path
                with requests.get(files_url, headers) as files_response:
                    files_response_json = files_response.json()
                    contents_url = ''
                    for file_json in files_response_json:
                        if file_json["filename"] == changelog_filename:
                            contents_url = file_json["contents_url"]
                            break
                    if len(contents_url) == 0:
                        raise Exception("No CHANGELOG.md file in PR files")
                    with requests.get(contents_url, headers) as contents_response:
                        contents_response_json = contents_response.json()
                        content = base64.b64decode(contents_response_json["content"]).decode("utf-8")
                        lines = content.split("\n")
                        versions = group_by_versions(lines)
                        unreleased_group = extract_unreleased_group(versions)
                        stable_versions = extract_stable_versions(versions)

                        for added_line in added_lines:
                            pr_link_matches = re.search(pr_link_regex, added_line)
                            if not pr_link_matches:
                                raise Exception("The changelog entry \"" + added_line + "\" should contain a link to the original PR that matches `" + pr_link_regex + "`")

                            if added_line not in unreleased_group:
                                raise Exception(added_line + " should be placed in 'Unreleased' section")

                            for stable_version in stable_versions:
                                if added_line in stable_versions[stable_version]:
                                    raise Exception("The changelog entry \"" + added_line + "\" is already contained in " + stable_version + " changelog.")
                print("Changelog entry validation successful.")
    else:
        print("`skip changelog` label present, exiting.")
