# NIV1984

Repository for NIV1984 Android app on Google Play Store: https://play.google.com/store/apps/details?id=com.aaronicsubstances.niv1984&hl=en

The text for the Holy Scriptures in NIV were taken from links similar to the one below for Paul's second letter to Timothy: 
http://web.mit.edu/jywang/www/cef/Bible/NIV/NIV_Bible/2TIM+1.html

## Building Android App

Create a **keystore.properties** file inside *Niv1984* folder to have the following properties:

* storePassword

* keyPassword

* keyAlias

* storeFile

### Build Environment

* NetBeans 8.1

* Android Studio 2.3.3

* Apache Maven 3.5.0

* JDK 1.8 (Oracle VM)

* Windows 8.1

### Running Preprocessor

At the root of the repo run

```bash
mvn -f nivbible-preprocessor\pom.xml clean package
cd scraped
mvn -f ..\nivbible-preprocessor\pom.xml exec:java -Dexec.mainClass=com.aaronicsubstances.nivbible.preprocessor.Main
```

to build the text of the Bible into files which are carried over to the Android app.