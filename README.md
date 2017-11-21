# dhis2-android-trackercapture
Android application for DHIS 2 for tracking of persons and things.

The refactored version of Tracker Capture, utilizing our new SDK is on the roadmap to be finished by 1st May 2016

Get the APK from the release page:

https://github.com/dhis2/dhis2-android-trackercapture/releases

# Testing
If you want to try the application out with a demo database, you can use the following:
- Server: https://apps.dhis2.org/demo
- Username: android
- Password: Android123

#How to Download and Set up in Android Studio
To successfully build and run this project, the dhis2-android-sdk is required.

Follow these Steps one by one:

1. The dhis2-android-sdk project https://github.com/hispindia/dhis2-android-sdk folder should be in the same root folder as the dhis2-android-sdk.
It should be on 2.23-legacy branch

2. Again Clone dhis2-android-sdk project https://github.com/hispindia/dhis2-android-sdk and create a new folder named as dhis2-android-new-sdk
Note that this should be on legacy-tracker branch.

3. Now CLone Trackercapture App from https://github.com/hispindia/dhis2-android-trackercapture folder should be dhis2-android-trackercapture.
It should be on 2.23-legacy branch

A workspace folder structure would look like this:

> dhis2-android-trackercapture(2.23-legacy)

>  dhis2-android-new-sdk(legacy-tracker)

> dhis2-android-sdk(2.23-legacy)




Then open Android Studio, select "Open an existing Android Studio project", and select the build.gradle in dhis2-android-trackercapture/
