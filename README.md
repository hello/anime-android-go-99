## anime-android-go-99

A small collection of utilities to make writing complex animations on Android less tedious.

## Prerequisites

- [Java](http://support.apple.com/kb/DL1572) (on Yosemite).
- [Android Studio](http://developer.android.com/sdk/index.html).
- The correct SDK and build tools. These will be automatically installed by Android Studio and the Square SDK manager gradle plugin.

## Deploying

To deploy Buruberi, you will need an access/private key pair for Hello's S3. After acquiring a key pair, you will need to add the following to your `$HOME/.gradle/gradle.properties` file:

    helloAwsAccessKeyID=<Your key id>
    helloAwsSecretKey=<Your secret key>

If you are not currently listed in the `developers` section under `publications`, add yourself before publishing.

To publish, set your current working directory to `buruberi` and enter the following:

    ./gradlew clean build publish

If the `VERSION_NAME` variable at the top of `anime/build.gradle` ends in `-SNAPSHOT`, the library will be published to the snapshot artifact repository. Otherwise, the library will be published to the releases repository. __Important:__ if you've made breaking changes to the API, make sure you increment the minor version code.
