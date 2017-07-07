# dhis2-android-trackercapture
Android application for DHIS 2 for tracking of persons and things.

Get the APK from the release page:

https://github.com/dhis2/dhis2-android-trackercapture/releases

# Testing
If you want to try the application out with a demo database, you can use the following:
- Server: https://apps.dhis2.org/demo
- Username: android
- Password: Android123

# How to Download and Set up in Android Studio
Stepwise explanation on how to set it up: https://docs.google.com/document/d/141uX2IKA7NRouaYDAPUhJu29WRmiw7UxwNtXSj_iOVA/edit?usp=sharing 

To successfully build and run this project, the dhis2-android-sdk is required.

The dhis2-android-sdk project https://github.com/dhis2/dhis2-android-sdk folder should be in the same root folder as the dhis2-android-trackercapture.

For 2.23-legacy branch: A workspace folder structure would look like this:

> .

> ..

> dhis2-android-sdk

> dhis2-android-trackercapture

> dhis2-android-new-sdk

dhis2-android-sdk project should be in branch 2.23-legacy. 
dhis2-android-trackercapture should be in branch 2.23-legacy.
dhis2-android-new-sdk should be in branch eventcapture

dhis2-android-new-sdk is the same project as dhis2-android-sdk but in a different branch. This means that you need to keep two sets of this project in your workspace. Since these two projects cannot have the same folder name the sdk-project in master branch should be renamed to dhis2-android-new-sdk

For older branches below 2.23: A workspace folder structure would look like this:

> .

> ..

> dhis2-android-sdk

> dhis2-android-trackercapture

Then open Android Studio, select "Open an existing Android Studio project", and select the build.gradle in dhis2-android-trackercapture/
