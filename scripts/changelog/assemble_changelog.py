import os

import git


def get_changes(path):
    changes = ''
    files = os.listdir(path)
    for file in files:
        pr_number = file.partition('.')[0]
        for line in open(path + file, 'r').readlines():
            line = line.replace('\n', '')
            if line.strip():
                changes += line + ' [#' + pr_number + '](https://github.com/mapbox/mapbox-navigation-android/pull/' + pr_number + ')' + '\n'
    return changes.strip()


bugfixes = get_changes('changelog/unreleased/bugfixes/')
features = get_changes('changelog/unreleased/features/')
issues = get_changes('changelog/unreleased/issues/')

changelog = '#### Features\n' + features + '\n\n' + \
            '#### Bug fixes and improvements\n' + bugfixes + '\n\n' + \
            '#### Known issues :warning:\n' + issues

old_changelog = open('changelog/unreleased/CHANGELOG.md', 'r').read()

if changelog != old_changelog:
    open('changelog/unreleased/CHANGELOG.md', 'w').write(changelog)
    repository = git.Repo('.')
    repository.git.add('changelog/unreleased')
    repository.index.commit('Assemble changelog file [skip actions]')
    repository.remotes.origin.push().raise_if_error()
