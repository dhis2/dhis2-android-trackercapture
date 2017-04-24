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

# How to Download and Set up in Android Studio
Want a step wise document on how to build this app? 
https://docs.google.com/document/d/141uX2IKA7NRouaYDAPUhJu29WRmiw7UxwNtXSj_iOVA/edit
To successfully build and run this project, the dhis2-android-sdk is required.

The dhis2-android-sdk project https://github.com/dhis2/dhis2-android-sdk folder should be in the same root folder as the dhis2-android-trackercapture.
A workspace folder structure would look like this:

> .

> ..

> dhis2-android-sdk

> dhis2-android-trackercapture

Then open Android Studio, select "Open an existing Android Studio project", and select the build.gradle in dhis2-android-trackercapture/
