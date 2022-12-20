import os

import git


def get_changes(path):
    changes = ''
    files = os.listdir(path)
    for file in files:
        changes += open(path + file, 'r').read() + '\n'
    return changes.strip()


bugfixes = get_changes('changelog/unreleased/bugfixes/')
features = get_changes('changelog/unreleased/features/')
issues = get_changes('changelog/unreleased/issues/')

changelog = '#### Features\n' + features + '\n\n' + \
            '#### Bug fixes and improvements\n' + bugfixes + '\n\n' + \
            '#### Known issues :warning:\n' + issues

open('changelog/unreleased/CHANGELOG.md', 'w').write(changelog)

repository = git.Repo('.')
repository.git.add('changelog/unreleased')
if len(repository.index.diff(None)) > 0:
    repository.index.commit('Assemble changelog file [skip actions]')
    repository.remotes.origin.push().raise_if_error()
