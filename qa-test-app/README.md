## Kaizen - カイゼン
*a Japanese business philosophy of continuous improvement of working practices, personal efficiency, etc.*

### Background
Though a great deal of effort is put into writing unit and instrumentation tests for the Navigation SDK the team decided there were some cases which required a visual assesment of the functionality of the SDK. The team decided to create a QA application so that we could consistently evalute various features to be sure there were no regressions during development. The application in this module called "Kaizen" serves that purpose.

### Usage
Launch the 'qa-test-app' module from android studio. Upon opening the Kaizen application you are presented with a list of test activities. Each activity has an 'info' label which gives a brief summary of what to observe in the activity.  The activities are not meant to be comprehensive navigation activities or represent best practices but instead are purpose built to test a specific feature. If you observe someting other than what is described a regression may of occurred and further investigation should be done.

### Contributing
To add additional testing activites simply create your activity along with the others in the module and provide an explanation for what's intended to be tested. Add your activity to the TestActivitySuite class along with the others as well as to the AndroidManifest.xml.

### Notes
In addition to the testing activites there are testing utilities included in this application. RouteDrawingActivity labeled "Internal Route Drawing Utility" makes it easy to create a route by long pressing points on a map. MapboxRouteLineActivity labeled "Route Line dev. activity" is used for quickly testing route line related features using an activity that doesn't change often (and probably shouldn't be changed).