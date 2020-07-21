#!/usr/bin/python

import os
import json

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

with open(licensePath, 'w') as licenseFile:
    licenseFile.write("The MIT License (MIT)\n\nCopyright (c) 2020 Mapbox\n\n")
    licenseFile.write("Permission is hereby granted, free of charge, to any person obtaining a copy\nof this software and associated documentation files (the \"Software\"), to deal\nin the Software without restriction, including without limitation the rights\nto use, copy, modify, merge, publish, distribute, sublicense, and/or sell\ncopies of the Software, and to permit persons to whom the Software is\nfurnished to do so, subject to the following conditions:\n\n")
    licenseFile.write("The above copyright notice and this permission notice shall be included in all\ncopies or substantial portions of the Software\n\n")
    licenseFile.write("THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR\nIMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,\nFITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE\nAUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER\nLIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,\nOUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE\nSOFTWARE.\n\n")
    licenseFile.write("## License of Dependencies\n\n")
    licenseFile.write("This SDK uses [Mapbox Navigator](https://github.com/mapbox/mapbox-navigation-android/blob/45b2aeb5f21fe8d008f533d036774dbe891252d4/libandroid-navigation/build.gradle#L47), a private binary, as a dependency. The Mapbox Navigator binary may be used with a\nMapbox account and under the [Mapbox TOS](https://www.mapbox.com/tos/). If you do not wish to use this binary, make sure you swap out this\ndependency in [libandroid-navigation/build.gradle](https://github.com/mapbox/mapbox-navigation-android/blob/master/libandroid-navigation/build.gradle). Code in this repo falls under the [MIT license](https://github.com/mapbox/mapbox-navigation-android/blob/master/LICENSE).\n\n")
    licenseFile.write("## Additional Mapbox Navigation Licenses\n\n")
    licenseFile.write("### Licenses are generated using:\n")
    licenseFile.write("---------------------------------------\n")
    project = "Gradle License Plugin"
    url = "https://github.com/jaredsburrows/gradle-license-plugin"
    license = "The Apache Software License, Version 2.0"
    license_url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
    licenseFile.write("URL: [%s](%s)  \n" % (project, url) + "License: [%s](%s)" % (license, license_url) + "\n\n\n")

    licenseFile.write("### Navigation SDK  \n")
    licenseFile.write("\n\n\n### Hybrid Router SDK module  \n")
    licenseFile.write("---------------------------------------\n")
    writeToFile(licenseFile, "/libdirections-hybrid/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n\n### Offboard Router SDK module  \n")
    licenseFile.write("---------------------------------------\n")
    writeToFile(licenseFile, "/libdirections-offboard/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n\n### Onboard Router SDK module  \n")
    licenseFile.write("---------------------------------------\n")
    writeToFile(licenseFile, "/libdirections-onboard/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n\n### Navigation Base SDK module  \n")
    licenseFile.write("---------------------------------------\n")
    writeToFile(licenseFile, "/libnavigation-base/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n\n### Navigation Core SDK module  \n")
    licenseFile.write("---------------------------------------\n")
    writeToFile(licenseFile, "/libnavigation-core/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n\n### Metrics SDK module  \n")
    licenseFile.write("---------------------------------------\n")
    writeToFile(licenseFile, "/libnavigation-metrics/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n\n### Navigator SDK module  \n")
    licenseFile.write("---------------------------------------\n")
    writeToFile(licenseFile, "/libnavigator/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n\n### Trip Notification SDK module  \n")
    licenseFile.write("---------------------------------------\n")
    writeToFile(licenseFile, "/libtrip-notification/build/reports/licenses/licenseReleaseReport.json")
    licenseFile.write("\n\n\n### Navigation UI SDK module  \n")
    licenseFile.write("---------------------------------------\n")
    writeToFile(licenseFile, "/libnavigation-ui/build/reports/licenses/licenseReleaseReport.json")

removeLicenseHtmlFileForModule("libdirections-hybrid")
removeLicenseHtmlFileForModule("libdirections-offboard")
removeLicenseHtmlFileForModule("libdirections-onboard")
removeLicenseHtmlFileForModule("libnavigation-base")
removeLicenseHtmlFileForModule("libnavigation-core")
removeLicenseHtmlFileForModule("libnavigation-metrics")
removeLicenseHtmlFileForModule("libnavigator")
removeLicenseHtmlFileForModule("libtrip-notification")
removeLicenseHtmlFileForModule("libnavigation-ui")