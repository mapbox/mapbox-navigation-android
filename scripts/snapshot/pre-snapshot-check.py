import datetime
import json
import os

import requests

from utils import is_snapshot_week, get_dependency_version, get_latest_tag, get_snapshot_branch


def build_message():
    message = '@navigation-android '

    if is_snapshot_week():
        message += 'Navigation SDK snapshot must be released today (rc or GA release was not released this week).\n'
    else:
        message += 'Navigation SDK snapshot must not be released today (rc or GA release was released this week).\n'
        return message

    maps_version = get_dependency_version('https://api.github.com/repos/mapbox/mapbox-maps-android-internal/releases')
    if maps_version:
        message += ':white-check-mark: Maps ' + maps_version + ' is ready.\n'
    else:
        message += ':siren: Expected Maps release was not released.\n'

    nav_native_version = get_dependency_version('https://api.github.com/repos/mapbox/mapbox-navigation-native/releases')
    if nav_native_version:
        message += ':white_check_mark: Nav Native ' + nav_native_version + ' is ready.\n'
    else:
        message += ':siren: Expected Nav Native release was not released.\n'

    tags = requests.get('https://api.github.com/repos/mapbox/mapbox-navigation-android/git/refs/tags').json()
    latest_tag = get_latest_tag(tags)
    snapshot_branch = get_snapshot_branch(latest_tag)

    message += 'Snapshot branch is *' + snapshot_branch + '*.\n'

    message += 'Snapshot name is *' + str(datetime.date.today()) + '_' + snapshot_branch + '*.\n'

    if maps_version and nav_native_version:
        message += '*Release time is today night.*\n'
    else:
        message += '*Snapshot will not be released until all necessary dependencies are released.*\n'

    return message


def send_message(message):
    payload = {'text': message, 'link_names': 1}
    slack_url = os.getenv("SLACK_WEBHOOK")
    requests.post(slack_url, data=json.dumps(payload))


message = build_message()
send_message(message)
