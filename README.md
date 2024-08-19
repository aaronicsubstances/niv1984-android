# NIV1984

Repository for NIV1984 Android app on Google Play Store: https://play.google.com/store/apps/details?id=com.aaronicsubstances.niv1984&hl=en

The text for the Holy Scriptures in NIV 1984 version were taken from links similar to the one below for Paul's second letter to Timothy: 
http://web.mit.edu/jywang/www/cef/Bible/NIV/NIV_Bible/2TIM+1.html
Also see http://web.mit.edu/jywang/www/cef/Bible/NIV/NIV_Bible/bookindex.html and http://web.mit.edu/jywang/www/cef/Bible/NIV/NIV_Bible/index.htm


## Building Android App

Create **app-debug.properties** and **app-release.properties** files besides the **app.properties** file for debug and release build variants respectively in order to specify a base API endpoint and credentials for use by mobile app.

Currently there's only one API involved, and that's the API for getting the latest version of the mobile app in order to force upgrades if necessary.

For release builds also create a **keystore.properties** file inside *Niv1984* folder to have the following properties:

* storePassword

* keyPassword

* keyAlias

* storeFile

### Build Environment

* Android Studio Koala
