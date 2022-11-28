import datetime
import json
import os

import requests

pr_number = os.environ['PR_NUMBER']
token = os.environ['GITHUB_TOKEN']

prs_url = "https://api.github.com/repos/mapbox/mapbox-navigation-android/pulls"
pr_url = prs_url + "/" + pr_number

headers = {"Authorization": "Bearer " + token}
pr = requests.get(pr_url, headers=headers).json()

if pr['draft']:
    print("It is draft pr")
    exit()

author = pr['user']['login']
current_reviewers = list(map(lambda reviewer: reviewer['login'], pr['requested_reviewers']))

# check existing approvals on pr

reviews_url = pr_url + "/reviews"
reviews = requests.get(reviews_url, headers=headers).json()
for review in reviews:
    if review['state'] == 'APPROVED':
        current_reviewers.append(review['user']['login'])

if len(current_reviewers) >= 2:
    print("2 or more reviewers already assigned")
    exit()

users = []

# parse users from config

with open('scripts/teams.json') as json_file:
    teams = json.load(json_file)
    for team in teams:
        team_name = team['name']
        for user in team['users']:
            if user == author:
                continue
            users.append({
                'login': user,
                'team': team_name,
                'reviews': 0,
                'done_reviews': 0
            })

# get users reviews

pulls = requests.get(prs_url, headers=headers).json()

for pull in pulls:
    reviewers = pull['requested_reviewers']
    for reviewer in reviewers:
        for user in users:
            if user['login'] == reviewer['login']:
                user['reviews'] += 1

# get users done reviews

closed_pulls_url = prs_url + "?state=closed&per_page=100"
closed_pulls = requests.get(closed_pulls_url, headers=headers).json()

today = datetime.date.today()

for pull in list(closed_pulls + pulls):
    if pull['closed_at'] is None:
        created_date = datetime.date.fromisoformat(pull['created_at'].partition('T')[0])
        if created_date + datetime.timedelta(days=7) < today:
            continue
    else:
        closed_date = datetime.date.fromisoformat(pull['closed_at'].partition('T')[0])
        if closed_date + datetime.timedelta(days=7) < today:
            continue
    pull_number = pull['number']
    reviews_url = prs_url + "/" + str(pull_number) + "/reviews"
    reviews = requests.get(reviews_url, headers=headers).json()
    for review in reviews:
        if review['state'] == 'APPROVED':
            for user in users:
                if user['login'] == review['user']['login']:
                    user['done_reviews'] += 1

# sort by reviews

users = sorted(users, key=lambda x: (x['reviews'], x['done_reviews']))

print("Available reviewers")
for user in users:
    print(user)

# get changes

pr_files_url = pr_url + '/files'
pr_files = requests.get(pr_files_url, headers=headers).json()
changed_modules = set(map(lambda reviewer: reviewer['filename'].split('/')[0], pr_files))

# find owners

found_owners = set()
with open('scripts/owners.json') as json_file:
    owners = json.load(json_file)
    for changed_module in changed_modules:
        for owner in owners:
            if changed_module in owner['modules']:
                for team in owner['teams']:
                    found_owners.add(team)

print("Owners of changes")
print(found_owners)

# find reviewers

found_reviewers = []

for user in users:
    if user['team'] in found_owners:
        found_reviewers.append(user['login'])
        break

if len(current_reviewers) + len(found_reviewers) < 2:
    for user in users:
        if user['login'] not in found_reviewers or user['team'] == "any":
            found_reviewers.append(user['login'])
            if len(current_reviewers) + len(found_reviewers) == 2:
                break

print("Reviewers to assign")
print(found_reviewers)

# assign reviewers

pr_url = prs_url + '/%s/requested_reviewers'
requests.post(pr_url % pr_number, json={'reviewers': found_reviewers}, headers=headers)
