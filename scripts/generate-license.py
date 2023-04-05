# encoding: utf-8
#!/usr/bin/python

import os
import json
import datetime
import codecs

path = os.getcwd()
licensePath = path + "/LICENSE.md"
htmlFileName = "/open_source_licenses.html"
assetsDirPath = "/src/main/assets"

class LicenseEntry:
    def __init__(self, project_name, project_descr, project_url, licence_name, license_url):
        self.project_name = project_name
        self.project_descr = project_descr
        self.project_url = project_url
        self.license_name = licence_name
        self.license_url = license_url

    def text(self):
        if self.project_descr is not None and str(self.project_descr).lower() != str(self.project_name).lower() :
            description = " (%s).\n" % self.project_descr
        else :
            description = ".\n"

        return ("Mapbox Navigation uses portions of the %s%s" % (self.project_name, description) +
                ("URL: [%s](%s)\n" % (self.project_url, self.project_url) if self.project_url is not None else "") +
                "License: [%s](%s)" % (self.license_name, self.license_url) +
                "\n\n===========================================================================\n\n")


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
                    for license in entry["licenses"]:
                        licenseName = license["license"]
                        licenseUrl = license["license_url"]

                    file.write(LicenseEntry(projectName, description, projectUrl, licenseName, licenseUrl).text())

staticLicenses = [
    LicenseEntry("Gradle License Plugin", None, "https://github.com/jaredsburrows/gradle-license-plugin", "The Apache Software License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt"),
    LicenseEntry("okhttp", "Square’s meticulous HTTP client for Java and Kotlin.", "https://square.github.io/okhttp/", "The Apache Software License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt")
]

with codecs.open(licensePath, 'w', encoding='utf-8') as licenseFile:
    now = datetime.datetime.now()

    licenseFile.write("### License\n")
    licenseFile.write("Mapbox Navigation for Android version 2.0\n\n")
    licenseFile.write("Mapbox Navigation Android SDK\n\n")
    licenseFile.write("Copyright " + u'©' + "2022 - {} Mapbox, Inc. All rights reserved.\n\n".format(now.year))
    licenseFile.write("The software and files in this repository (collectively, \"Software\") are licensed under the Mapbox TOS for use only with the relevant Mapbox product(s) listed at www.mapbox.com/pricing. This license allows developers with a current active Mapbox account to use and modify the authorized portions of the Software as needed for use only with the relevant Mapbox product(s) through their Mapbox account in accordance with the Mapbox TOS.  This license terminates automatically if a developer no longer has a Mapbox account in good standing or breaches the Mapbox TOS. For the license terms, please see the Mapbox TOS at https://www.mapbox.com/legal/tos/ which incorporates the Mapbox Product Terms at www.mapbox.com/legal/service-terms.  If this Software is a SDK, modifications that change or interfere with marked portions of the code related to billing, accounting, or data collection are not authorized and the SDK sends limited de-identified location and usage data which is used in accordance with the Mapbox TOS. [Updated 2023-03]\n\n")
    licenseFile.write("---------------------------------------\n")
    [licenseFile.write(l.text()) for l in staticLicenses]

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
    licenseFile.write("\n\n#### Navigation UI Voice SDK module\n")
    writeToFile(licenseFile, "/libnavui-voice/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n#### Navigation group module\n")
    writeToFile(licenseFile, "/libnavigation-android/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n#### Navigation UI speed limit module\n")
    writeToFile(licenseFile, "/libnavui-speedlimit/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n#### Navigation UI shield module\n")
    writeToFile(licenseFile, "/libnavui-shield/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n#### Navigation UI status module\n")
    writeToFile(licenseFile, "/libnavui-status/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n#### Navigation UI Drop-In module\n")
    writeToFile(licenseFile, "/libnavui-dropin/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n#### Navigation UI Android Auto module\n")
    writeToFile(licenseFile, "/libnavui-androidauto/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n#### Navigation UI App SDK module\n")
    writeToFile(licenseFile, "/libnavui-app/build/reports/licenses/licenseReleaseReport.json")

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
removeLicenseHtmlFileForModule("libnavui-voice")
removeLicenseHtmlFileForModule("libnavigation-android")
removeLicenseHtmlFileForModule("libnavui-speedlimit")
removeLicenseHtmlFileForModule("libnavui-shield")
removeLicenseHtmlFileForModule("libnavui-status")
removeLicenseHtmlFileForModule("libnavui-androidauto")
removeLicenseHtmlFileForModule("libnavui-app")
removeLicenseHtmlFileForModule("libnavui-dropin")
