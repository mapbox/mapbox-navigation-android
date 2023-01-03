import os

import git


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

old_changelog = open('changelog/unreleased/CHANGELOG.md', 'r').read()

if changelog != old_changelog:
    open('changelog/unreleased/CHANGELOG.md', 'w').write(changelog)
    repository = git.Repo('.')
    repository.git.add('changelog/unreleased')
    repository.index.commit('Assemble changelog file [skip actions]')
    repository.remotes.origin.push().raise_if_error()

auto_bugfixes = get_changes('libnavui-androidauto/changelog/unreleased/bugfixes/')
auto_features = get_changes('libnavui-androidauto/changelog/unreleased/features/')

auto_changelog = '#### Features\n' + auto_features + '\n\n' + \
            '#### Bug fixes and improvements\n' + auto_bugfixes

auto_old_changelog = open('libnavui-androidauto/changelog/unreleased/CHANGELOG.md', 'r').read()

if auto_changelog != auto_old_changelog:
    open('libnavui-androidauto/changelog/unreleased/CHANGELOG.md', 'w').write(auto_changelog)
    repository = git.Repo('.')
    repository.git.add('libnavui-androidauto/changelog/unreleased')
    repository.index.commit('Assemble auto changelog file [skip actions]')
    repository.remotes.origin.push().raise_if_error()
