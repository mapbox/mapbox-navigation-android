import requests


def fetch_pull_request(pr_number, token):
    prs_url = "https://api.github.com/repos/mapbox/mapbox-navigation-android/pulls"
    pr_url = prs_url + "/" + pr_number

    headers = {"Authorization": "Bearer " + token}
    return requests.get(pr_url, headers=headers).json()

def is_draft(pr):
    return pr['draft']

def is_labeled(pr, label_name):
    return len([label for label in pr['labels'] if label["name"] == label_name]) > 0
