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

To successfully build and run this project, the dhis2-android-sdk is required.

The dhis2-android-sdk project https://github.com/dhis2/dhis2-android-sdk folder should be in a subfolder called named sdk inside dhis2-android-trackercapture. It is configured as a git submodule, so it will be automatically include when cloned using --recursive. 

To be compatible with 2.25/2.26/2.27 & 2.28 servers, use 2.23-legacy branch in dhis2-android-trackercapture and tracker-capture branch in dhis2-android-sdk repositories.

When cloning from zero, it's strongly recommended to do it as follows:

```
git clone --recursive -b 2.23-legacy git@github.com:dhis2/dhis2-android-trackercapture.git
cd dhis2-android-trackercapture/sdk
git checkout tracker-capture
```

Then open Android Studio, select "Open an existing Android Studio project", and select the build.gradle in dhis2-android-trackercapture/
