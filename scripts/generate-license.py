# encoding: utf-8
#!/usr/bin/python

import os
import json
import codecs

path = os.getcwd()
licensePath = path + "/LICENSE.md"
htmlFileName = "/open_source_licenses.html"
assetsDirPath = "/src/main/assets"

def removeLicenseHtmlFileForModule(moduleName) :
    try:
        os.remove(path + "/" + moduleName + assetsDirPath + htmlFileName)
        removeAssetsDirIfEmpty(moduleName)
    except:
        print("Error while deleting %s file in %s module" % (htmlFileName, moduleName))

def removeAssetsDirIfEmpty(moduleName) :
    try:
        os.rmdir(path + "/" + moduleName + assetsDirPath)
    except:
        print("Assets is not empty in module %s" % moduleName)

def writeToFile(file, filePath) :
    with open(path + filePath, 'r') as dataFile:
            data = json.load(dataFile)
            licenseName = ""
            licenseUrl = ""
            uniqueProjects = set()
            for entry in data:
                projectName = entry["project"]
                if not projectName in uniqueProjects :
                    uniqueProjects.add(projectName)
                    projectUrl = entry["url"]
                    description = entry["description"]
                    if description is not None and str(description).lower() != str(projectName).lower() :
                        description = " (%s).\n" % description
                    else :
                        description = ".\n"
                    for license in entry["licenses"]:
                        licenseName = license["license"]
                        licenseUrl = license["license_url"]

                    file.write("Mapbox Navigation uses portions of the %s%s" % (projectName, description) +
                                      ("URL: [%s](%s)\n" % (projectUrl, projectUrl) if projectUrl is not None else "") +
                                      "License: [%s](%s)" % (licenseName, licenseUrl) +
                                      "\n\n===========================================================================\n\n")

with codecs.open(licensePath, 'w', encoding='utf-8') as licenseFile:
    licenseFile.write("### License\n")
    licenseFile.write("Mapbox Navigation for Android version 2.0\n\n")
    licenseFile.write("Mapbox Navigation Android SDK\n\n")
    licenseFile.write("Copyright " + u'©' + "2021 Mapbox\n\n")
    licenseFile.write("All rights reserved.\n\n")
    licenseFile.write("Mapbox Navigation for Android version 2.0 (" + u"“" + "Mapbox Navigation Android SDK" + u"“" + ") or higher must be used according to the Mapbox Terms of Service. This license allows developers with a current active Mapbox account to use and modify the Mapbox Navigation Android SDK. Developers may modify the Mapbox Navigation Android SDK code so long as the modifications do not change or interfere with marked portions of the code related to billing, accounting, and anonymized data collection. The Mapbox Navigation Android SDK sends anonymized location and usage data, which Mapbox uses for fixing bugs and errors, accounting, and generating aggregated anonymized statistics. This license terminates automatically if a user no longer has an active Mapbox account.\n\n")
    licenseFile.write("For the full license terms, please see the Mapbox Terms of Service at https://www.mapbox.com/legal/tos/\n\n")
    licenseFile.write("---------------------------------------\n")
    project = "Gradle License Plugin"
    url = "https://github.com/jaredsburrows/gradle-license-plugin"
    license = "The Apache Software License, Version 2.0"
    license_url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
    licenseFile.write("URL: [%s](%s)  \n" % (project, url) + "License: [%s](%s)" % (license, license_url))

    licenseFile.write("\n\n#### Hybrid Router SDK module\n")
    writeToFile(licenseFile, "/libnavigation-router/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n#### Navigation Base SDK module\n")
    writeToFile(licenseFile, "/libnavigation-base/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n#### Navigation Core SDK module\n")
    writeToFile(licenseFile, "/libnavigation-core/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n#### Metrics SDK module\n")
    writeToFile(licenseFile, "/libnavigation-metrics/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n#### Navigator SDK module\n")
    writeToFile(licenseFile, "/libnavigator/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n#### Trip Notification SDK module\n")
    writeToFile(licenseFile, "/libtrip-notification/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n#### Navigation UI Base SDK module\n")
    writeToFile(licenseFile, "/libnavui-base/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n#### Navigation UI Maps SDK module\n")
    writeToFile(licenseFile, "/libnavui-maps/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n#### Navigation UI Util SDK module\n")
    writeToFile(licenseFile, "/libnavui-util/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n#### Navigation UI Trip Progress SDK module\n")
    writeToFile(licenseFile, "/libnavui-tripprogress/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n#### Navigation UI Maneuver SDK module\n")
    writeToFile(licenseFile, "/libnavui-maneuver/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n#### Navigation UI Resources SDK module\n")
    writeToFile(licenseFile, "/libnavui-resources/build/reports/licenses/licenseReleaseReport.json")

removeLicenseHtmlFileForModule("libnavigation-router")
removeLicenseHtmlFileForModule("libnavigation-base")
removeLicenseHtmlFileForModule("libnavigation-core")
removeLicenseHtmlFileForModule("libnavigation-metrics")
removeLicenseHtmlFileForModule("libnavigator")
removeLicenseHtmlFileForModule("libtrip-notification")
removeLicenseHtmlFileForModule("libnavui-base")
removeLicenseHtmlFileForModule("libnavui-maps")
removeLicenseHtmlFileForModule("libnavui-util")
removeLicenseHtmlFileForModule("libnavui-tripprogress")
removeLicenseHtmlFileForModule("libnavui-maneuver")
removeLicenseHtmlFileForModule("libnavui-resources")