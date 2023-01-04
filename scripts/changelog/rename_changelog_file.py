import os
import re
import git
from pr_utils import *

pr_number = os.environ['PR_NUMBER']
token = os.environ['GITHUB_TOKEN']

pattern = re.compile("^\d*.md")


def rename_files(path):
    if not os.path.isdir(path):
        return 0

    renamed_files_count = 0
    files = os.listdir(path)

    new_md_files = list(filter(lambda file: not pattern.match(file), files))

    if len(new_md_files) > 1:
        raise Exception('More than one new changelog file')

    for file in new_md_files:
        if not pattern.match(file):
            os.rename(path + file, path + pr_number + '.md')
            renamed_files_count += 1

    return renamed_files_count


pr = fetch_pull_request(pr_number, token)
if is_draft(pr):
    print("SKIP! Pull Request is a DRAFT.")
    exit()

if is_labeled(pr, "skip changelog"):
    print("SKIP! Pull Request labeled 'skip changelog'.")
    exit()

renamed_bugfixes_count = rename_files('changelog/unreleased/bugfixes/')
renamed_features_count = rename_files('changelog/unreleased/features/')

auto_renamed_bugfixes_count = rename_files('libnavui-androidauto/changelog/unreleased/bugfixes/')
auto_renamed_features_count = rename_files('libnavui-androidauto/changelog/unreleased/features/')

if renamed_features_count + renamed_bugfixes_count + auto_renamed_bugfixes_count + auto_renamed_features_count > 0:
    repository = git.Repo('.')
    repository.git.add('changelog/unreleased')
    repository.git.add('libnavui-androidauto/changelog/unreleased')
    repository.index.commit('Rename changelog files')
    repository.remotes.origin.push().raise_if_error()
