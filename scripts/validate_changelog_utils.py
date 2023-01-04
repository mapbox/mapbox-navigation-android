import re

changelog_diff_regex = "^([\s]*)diff --git a/changelog/unreleased/(features|bugfixes|issues|other)/(.*).md b/changelog/unreleased/(features|bugfixes|issues|other)/(.*).md"
changelog_diff_filename_regex = re.compile("([\s]*)diff --git a\/(.*) b\/(.*)")
changelog_filename = "CHANGELOG.md"
any_diff_substring = "diff --git"
diff_file_start_regex = "^([\s]*)\@\@(.*)\@\@"
pr_link_regex = "\[#\d+]\(https:\/\/github\.com\/mapbox\/mapbox-navigation-android\/pull\/\d+\)"


def is_line_added(line):
    return line.strip().startswith('+')


def is_line_not_blank(line):
    return len(line.strip()) > 0


def remove_plus(line):
    return line.strip()[1:]


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
        elif is_line_not_blank(line):
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


def should_skip_changelog(response_json):
    if "labels" in response_json:
        pr_labels = response_json["labels"]
        for label in pr_labels:
            if label["name"] == "skip changelog":
                return True
    return False


def check_has_changelog_diff(diff):
    changelog_diff_matches = re.search(changelog_diff_regex, diff, re.MULTILINE)
    if not changelog_diff_matches:
        raise Exception(
            "Add a non-empty changelog file in changelog/unreleased/${type of changes} or add a `skip changelog` label if not applicable.")


def parse_contents_url(files_response_json):
    content_urls = {}
    for file_json in files_response_json:
        filename = file_json["filename"]
        if filename == changelog_filename or filename.endswith("/" + changelog_filename):
            content_urls[filename] = file_json["contents_url"]
    if len(content_urls) == 0:
        raise Exception("No CHANGELOG.md file in PR files")
    return content_urls


def extract_added_lines(whole_diff):
    added_lines = {}
    diff = whole_diff
    while len(diff) > 0:
        changelog_diff_matches = re.search(changelog_diff_regex, diff, re.MULTILINE)
        if not changelog_diff_matches:
            break
        matched_changelog_diff = diff[changelog_diff_matches.start():changelog_diff_matches.end()]
        filename = re.match(changelog_diff_filename_regex, matched_changelog_diff).group(2)

        diff_starting_at_changelog = diff[changelog_diff_matches.end():]
        first_changelog_diff = re.search(diff_file_start_regex, diff_starting_at_changelog, re.MULTILINE)
        first_changelog_diff_index = 0
        if first_changelog_diff:
            first_changelog_diff_index = first_changelog_diff.end()
        last_reachable_index = diff_starting_at_changelog.find(any_diff_substring, first_changelog_diff_index)
        if last_reachable_index == -1:
            last_reachable_index = len(diff_starting_at_changelog)
        diff_searchable = diff_starting_at_changelog[first_changelog_diff_index:last_reachable_index]

        diff_lines = diff_searchable.split('\n')
        added_lines[filename] = list(
            filter(is_line_not_blank, map(remove_plus, list(filter(is_line_added, diff_lines)))))
        diff = diff[last_reachable_index:]
    return added_lines


def check_for_duplications(added_lines):
    unique_added_lines = set()
    for added_line in added_lines:
        if added_line in unique_added_lines:
            raise Exception("\"" + added_line + "\" is added more than once")
        else:
            unique_added_lines.add(added_line)
