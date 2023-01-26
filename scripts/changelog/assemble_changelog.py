import os

import requests

pr_number = os.environ['PR_NUMBER']
token = os.environ['GITHUB_TOKEN']


def get_changes(path):
    changes = ''
    if not os.path.isdir(path):
        return ''
    files = os.listdir(path)
    for file in files:
        pr_number = file.partition('.')[0]
        pr_changes = open(path + file, 'r').read()
        if path.endswith('bugfixes/') or path.endswith('features/'):
            pr_link = ' [#' + pr_number + '](https://github.com/mapbox/mapbox-navigation-android/pull/' + pr_number + ')' + '\n'
            lines_with_description = []
            for line in open(path + file, 'r').readlines():
                if line.startswith('- '):
                    lines_with_description.append(line)
            for line in lines_with_description:
                pr_changes = pr_changes.replace(line, line.replace('\n', '') + pr_link)
        if not pr_changes.endswith('\n'):
            pr_changes += '\n'
        changes += pr_changes
    return changes.strip()


bugfixes = get_changes('changelog/unreleased/bugfixes/')
features = get_changes('changelog/unreleased/features/')
issues = get_changes('changelog/unreleased/issues/')
other = get_changes('changelog/unreleased/other/')

changelog = '#### Features\n' + features + '\n\n' + \
            '#### Bug fixes and improvements\n' + bugfixes + '\n\n' + \
            '#### Known issues :warning:\n' + issues + '\n\n' + \
            '#### Other changes\n' + other

auto_bugfixes = get_changes('libnavui-androidauto/changelog/unreleased/bugfixes/')
auto_features = get_changes('libnavui-androidauto/changelog/unreleased/features/')

auto_changelog = '#### Features\n' + auto_features + '\n\n' + \
                 '#### Bug fixes and improvements\n' + auto_bugfixes

pr_comments_url = 'https://api.github.com/repos/mapbox/mapbox-navigation-android/issues/' + pr_number + '/comments'
headers = {"Authorization": "Bearer " + token}
comments = requests.get(pr_comments_url, headers=headers).json()

full_changelog = '<details>\n<summary>Changelog</summary>\n\n```\n' + \
                 changelog + '\n```\n</details>\n' + \
                 '<details>\n<summary>Android Auto Changelog</summary>\n\n```\n' + \
                 auto_changelog + '\n```\n</details>'

comment_with_changelog_id = None
for comment in comments:
    if comment['body'].startswith('<details>\n<summary>Changelog</summary>\n'):
        comment_with_changelog_id = comment['id']

if comment_with_changelog_id:
    comments_url = 'https://api.github.com/repos/mapbox/mapbox-navigation-android/issues/comments/'
    comment_url = comments_url + str(comment_with_changelog_id)
    requests.patch(comment_url, json={'body': full_changelog}, headers=headers)
else:
    requests.post(pr_comments_url, json={'body': full_changelog}, headers=headers)
