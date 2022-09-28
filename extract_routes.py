# encoding: utf-8
# !/usr/bin/python
import json
import os
import sys

root_path = os.getcwd() + "/"


def main():
    history_file_relative_path = sys.argv[1]
    history_file_relative_path_segments = history_file_relative_path.split("/")
    history_file_name = history_file_relative_path_segments[len(history_file_relative_path_segments) - 1]
    print("name: " + history_file_name)
    with open(root_path + history_file_relative_path) as history_file_json:
        history_json = json.load(history_file_json)
        events = history_json["events"]
        index = 0
        for event in events:
            if event["type"] == "setRoute":
                text_file = open(root_path + history_file_name + "_route_" + str(index) + ".json", "w")
                text_file.write(event["route"])
                text_file.close()
                print("route_" + str(index) + "_uri: " + event["requestUri"])
                index += 1


if __name__ == "__main__":
    main()
