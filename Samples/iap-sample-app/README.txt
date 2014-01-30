---------------------------------------------------------------
Familiarize yourself with the pieces of the Sample App
---------------------------------------------------------------

The Sample App has been provided to help familiarize you with the OUYA ODK and is made up of the following files and directories:

 - AndroidManifest.xml: Essential information about the application to the Android system, information the system must have before it can run any of the application's code.  See http://developer.android.com/guide/topics/manifest/manifest-intro.html for further specifics.
 - ant.properties: Overrides default values used by the Ant build system
 - build.xml: Ant build script
 - find-android.sh: Creates the local.properties file. The tests will not run if local.properties is missing or invalid.
 - project.properties: The Android build system reads this file. You should not need to change it.
 - res: Android resources tree. http://developer.android.com/guide/topics/resources/available-resources.html has more information.
 - src: Java source code for the in-app purchase sample application.
 - test: Java source code for the unit tests for the in-app purchase sample application.
 - tmp: This directory is empty. After you've run the tests, you'll see a .jar file in this directory. You should not need to change this.

----------------------------------------------------------
Make developer-specific changes to the Sample App
----------------------------------------------------------

1. Login to the Developer Portal at https://developer.ouya.tv and make a note of your developer UUID:
2. Navigate to Products and add two or more products.  For at least one of these products, the product identifiers should start with "__DECLINED__"
3. Find and edit the following file: src => java => tv => ouya => sample => IapSampleActivity.java
 - Change DEVELOPER_ID to match the UUID that you noted from the Developer Portal.
 - Remove the existing strings from PRODUCT_IDENTIFIER_LIST and update it with the list of product identifiers you created in step 2 of this section.  They must match the product identifiers you defined in the Developer Portal exactly.

-----------------------------------------
Compile and build the Sample App
-----------------------------------------

1. From a terminal window, navigate to the top-level directory of the sample code (where this README resides)
2. Run "android update project --path ."
3. Run "ant clean test" to compile the code and run the automated tests.

If you see any test failures, look at the appropriate test output file in the bin/out/reports/tests directory.

4. If the tests run successfully, run "ant debug" to create an APK.

----------------------------------
Installing The Sample APKs On Your Device
----------------------------------

You should now have two APKs to install: the Sample App, which you just built, and the OUYA Launcher, which talks to the server on your behalf.

1) Run adb install -r ouya-launcher.apk to install the launcher.

2) Run adb install -r ouya-framework.apk to install the OUYA framework.

3) Run adb install -r iap-sample-app/bin/OuyaAndroid-debug.apk to install the sample app.

-----------------------
Use the Sample App
-----------------------

The Sample App should be visible under the name "IAP Sample App" in the Apps screen on your device.

 - Tap to start the app and sign in with your Developer Portal credentials.
 - The buttons at the top should correspond to the product identifiers you added to IapSampleActivity.java and on the server through the Developer Portal.
 - Tapping on each of the buttons will allow you to purchase one of the products and see the flow.
   - Some popups/dialogs you'll see are built into the IAP service, some are part of the Sample App.  The latter can be distinguished by a "IAP Sample APP" header.
   - When the user taps on one of the products, the Sample App turns processing over to the IAP service, which presents a confirmation dialog.
   - If the user confirms the purchase, a request is sent from the IAP service to the server, which returns a success/failure message.
   - This response is conveyed back to the Sample App by the IAP service.
   - Based on this response, the Sample App displays a purchase succeeded popup or a failed dialog.
   - The IAP service also provides a response back to the Sample App when the purchase is canceled.  The Sample App the displays a failed dialog in this case.
   - Note that when you attempt to purchase a product whose product identifier starts with "__DECLINED__", the server/service will always return a payment declined error.  These special product identifiers allow you to test for the failed case.
 - Tapping on the "Request Gamer UUID" button at the bottom will return an ID for the currently logged-in gamer.  This ID is unique to each gamer on a per-developer basis (meaning the server will return a different ID for each developer that requests it for the same gamer).

Known issues:

 - Some Android devices display a strange character (unicode value 0xA4) where a dollar-sign belongs. This appears to be a bug in the device's default Locale.








