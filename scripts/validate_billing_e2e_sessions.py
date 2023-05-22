import urllib.request
import json
import datetime
import os

def validatePeriod(period):
    periodStartTime = datetime.datetime.fromtimestamp(period[0]//1000)
    periodEndTime = datetime.datetime.fromtimestamp(period[1]//1000)
    today = datetime.datetime.now()
    if periodStartTime <= today <= periodEndTime:
        return True
    else:
        return False

token = os.environ["MAPBOX_BILLING_STATISTICS_ACCESS_TOKEN"]
url = f"https://api.mapbox.com/billing/usage/v1?access_token={token}"

response = urllib.request.urlopen(url)

if response.status != 200:
    raise ValueError(f"Request failed: {response.status}")

data = json.loads(response.read().decode("utf-8"))

period = data["period"]
if validatePeriod(period) != True:
    raise ValueError(f"Current date is not within data period. Period: {period}, current date: {datetime.datetime.now()}")

# The number of billing sessions should match the current day of the month minus one because
# billing tests CI job creates one billing event per day.
# However, there is a lag between the creation and the storage of the billing event in the database,
# so we exclude the most recent billing event from the count.
# More details can be found in the issue NAVAND-1193

numberOfSessions = data["nav2sestrip"]
expectedNumberOfSessionTrips = datetime.date.today().day - 1
if numberOfSessions != expectedNumberOfSessionTrips:
    raise ValueError(f"Expected {expectedNumberOfSessionTrips} sessions, but was {numberOfSessions}")
else:
    print(f"Test passed. Number of sessions: {numberOfSessions}")
