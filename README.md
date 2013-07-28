DAS.UHR App
===========

This repository contains the code for the DAS.UHR Android app. The app can be used to interact with
the DAS.UHR word clock.

The app is available in Google's Play Store for free: https://play.google.com/store/apps/details?id=com.daspilker.uhr.app

To compile the App you need a recent version of Java 7 and the Android SDK. Set the `ANDROID_HOME`
environment variable to point to the Android SDK installation. Then call the `gradlew` script. After
building, the app`s APK file can be found in the `build/apk` directory. Copy the APK file to an
Android device and launch the APK to install the app. You may need to allow installation of apps
from untrusted sources in the Android security settings menu. The Android device needs to be paired
manually with the clock before using the app. Use the Android Bluetooth settings to pair your
device with the clock. The clock's bluetooth device name is `DAS.UHR` and the pin is `1234`.


License
-------

Copyright 2013 Daniel A. Spilker

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
