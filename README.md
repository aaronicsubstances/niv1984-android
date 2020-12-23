# NIV1984

Repository for NIV1984 Android app on Google Play Store: https://play.google.com/store/apps/details?id=com.aaronicsubstances.niv1984&hl=en

The text for the Holy Scriptures in NIV were taken from http://web.mit.edu/jywang/www/cef/Bible/NIV/NIV_Bible/index.htm

The text for the Holy Scriptures in KJV were taken from https://www.kingjamesbibleonline.org
        
## Building Android App

Create **app-debug.properties** and **app-release.properties** files besides the **app.properties** file for debug and release build variants respectively in order to specify a base API endpoint and credentials for use by mobile app.

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


### ImageMagick Commands

#### cut round circle from image

```
magick convert -size 144x144 xc:Black -fill White -draw 'circle 72 72 72 3' -alpha Copy mask.png
```

```
magick convert ic_launcher.png -gravity Center mask.png -compose CopyOpacity -composite -trim ic_launcher_round.png
```

```
magick convert -border 2 -bordercolor transparent ic_launcher_round.png outputimage.png
```

```
magick convert -extent 144X144 -background transparent outputimage.png y.png 
```
