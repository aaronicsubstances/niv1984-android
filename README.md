# NIV1984

Repository for NIV1984 Android app on Google Play Store: https://play.google.com/store/apps/details?id=com.aaronicsubstances.niv1984&hl=en

The text for the Holy Scriptures in NIV were taken from links similar to the one below for Paul's second letter to Timothy: 
http://web.mit.edu/jywang/www/cef/Bible/NIV/NIV_Bible/2TIM+1.html

## Building Android App

Create a *ConfigUtils.java* source code file in the *com.aaronicsubstances.niv1984* package such as below

```java
package com.aaronicsubstances.niv1984;

public class ConfigUtils {
    public static final String API_BASE_URL = "http://www.aaronicsubstances.com/niv1984/api";
    public static final String API_CRED = "Basic blahblahblah";
}

```

to specify a base API endpoint and credentials for use by mobile app.

Currently there's only one API involved, and that's the API for getting the latest version of the mobile in order to force upgrades if necessary. That API returns the latest version in plain text and optionally prefixes the latest version with an asterisk to force upgrades. E.g. 1.2.0, *1.3

For release builds create a **keystore.properties** file inside *Niv1984* folder to have the following properties:

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