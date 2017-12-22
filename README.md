# NIV1984

Repository for NIV1984 Android app on Google Play Store: https://play.google.com/store/apps/details?id=com.aaronicsubstances.niv1984&hl=en

The text for the Holy Scriptures in NIV were taken from links similar to the one below for Paul's second letter to Timothy: 
http://web.mit.edu/jywang/www/cef/Bible/NIV/NIV_Bible/2TIM+1.html

The text for the Holy Scriptures in KJV were taken from the mcb-kjv distribution used in MySQL Cookbook 3.0: http://www.kitebird.com/mysql-cookbook/

## Building Android App

Create an **app-release.properties** file besides the **app-debug.properties** file for release builds in order to specify a base API endpoint and credentials for use by mobile app.

Currently there's only one API involved, and that's the API for getting the latest version of the mobile app in order to force upgrades if necessary.

For release builds also create a **keystore.properties** file inside *Niv1984* folder to have the following properties:

* storePassword

* keyPassword

* keyAlias

* storeFile

### Build Environment

* NetBeans 8.2

* Android Studio 2.3.3

* Apache Maven 3.5.0

* JDK 1.8 (Oracle VM)

* Windows 8.1