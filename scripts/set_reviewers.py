import requests, os

pr_number = os.environ['PR_NUMBER']
token = os.environ['GITHUB_WRITER_TOKEN']

pr_url = "https://api.github.com/repos/mapbox/mapbox-navigation-android/pulls/" + pr_number

headers = {"Authorization": "Bearer " + token}
pr = requests.get(pr_url, headers=headers).json()

print(pr)
