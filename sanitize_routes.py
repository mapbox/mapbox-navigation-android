# encoding: utf-8
# !/usr/bin/python
import json
import os
import sys

root_path = os.getcwd() + "/"


def sanitize(v):
    value_type = type(v)
    if value_type is dict:
        new_dict = dict()
        for key, value in v.items():
            new_dict[key] = sanitize(value)
        return new_dict
    else:
        if value_type is list:
            return list(map(sanitize, v))
        else:
            if value_type is bool:
                return v
            else:
                try:
                    number = float(v)
                    if number.is_integer():
                        return int(number)
                    else:
                        return number
                except Exception:
                    if v == "null":
                        return None
                    else:
                        return v


def main():
    file_relative_path = sys.argv[1]
    destination_relative_path = ""
    if len(sys.argv) > 1:
        destination_relative_path = sys.argv[2] + "/"
    file_relative_path_segments = file_relative_path.split("/")
    file_name = file_relative_path_segments[len(file_relative_path_segments) - 1]
    print("name: " + file_name)
    with open(root_path + file_relative_path) as file_json:
        json_obj = json.load(file_json, object_hook=sanitize)
        json_string = json.dumps(json_obj)
        text_file = open(root_path + destination_relative_path + "sanitized_" + file_name, "w")
        text_file.write(str(json_string))
        text_file.close()


if __name__ == "__main__":
    main()
