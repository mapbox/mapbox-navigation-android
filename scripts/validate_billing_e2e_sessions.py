import os
import urllib.request
import json
import time
import datetime

def dateToMillis(date):
    return int(time.mktime(date.timetuple()) * 1000)

token = os.environ["MAPBOX_BILLING_STATISTICS_ACCESS_TOKEN"]

periodEndTime = datetime.datetime.now()
periodEndTimeMillis = dateToMillis(periodEndTime)

periodStartTime = periodEndTime - datetime.timedelta(days=1)
periodStartTimeMillis = dateToMillis(periodStartTime)

print(f"Retrieving billing events for the time interval {periodStartTime} - {periodEndTime}")

url = f"https://api.mapbox.com/billing/usage/v1?access_token={token}&period_start={periodStartTimeMillis}&period_end={periodEndTimeMillis}"

response = urllib.request.urlopen(url)

if response.status != 200:
    raise ValueError(f"Request failed: {response.status}")

data = json.loads(response.read().decode("utf-8"))

numberOfSessions = data["nav2sestrip"]

# We expect one session per requested period
if numberOfSessions != 1:
    raise ValueError(f"Expected 1 session, but was {numberOfSessions}")
else:
    print(f"Test passed.")
