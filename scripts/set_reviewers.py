import requests, os

pr_number = os.environ['PR_NUMBER']
token = os.environ['GITHUB_TOKEN']

pr_url = "https://api.github.com/repos/mapbox/mapbox-navigation-android/pulls/" + pr_number

headers = {"Authorization": "Bearer " + token}
pr = requests.get(pr_url, headers=headers).json()

if pr['draft']:
    print("It is draft pr")
    exit()

author = pr['user']['login']
current_reviewers = list(map(lambda reviewer: reviewer['login'], pr['requested_reviewers']))

# check existing approvals on pr

reviews_url = "https://api.github.com/repos/mapbox/mapbox-navigation-android/pulls/" + pr_number + "/reviews"
reviews = requests.get(reviews_url, headers=headers).json()
for review in reviews:
    if review['state'] == 'APPROVED':
        current_reviewers.append(review['user']['login'])

if len(current_reviewers) >= 2:
    print("2 or more reviewers already assigned")
    exit()

users = []
