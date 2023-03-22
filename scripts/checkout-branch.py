import subprocess

import requests

tags = requests.get('https://api.github.com/repos/mapbox/mapbox-navigation-android/git/refs/tags').json()

for tag in reversed(tags):
    tag_name = tag['ref'].replace('refs/tags/', '')
    if tag_name.startswith('v') and tag_name.partition('-')[0].endswith('.0'):
        latest_no_patch_tag = tag_name
        break
print('Latest no-patch release is ' + latest_no_patch_tag)

branch_name = 'release-' + latest_no_patch_tag.partition('.0')[0]
print('Checking if ' + branch_name + ' branch exists...')

branch_response = requests.get('https://api.github.com/repos/mapbox/mapbox-navigation-android/branches/' + branch_name)

if branch_response.ok:
    print(branch_name + ' exists. Checking out...')
    subprocess.run("git checkout $release_branch", shell=True, check=True)
else:
    print(branch_name + ' does not exist.')
