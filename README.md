# PermanentProxy-for-WearOS
Simple app to run a proxy (on boot) on a Wear OS device and enable geo-restricted apps like Google Pay in unsupported regions!

(The apk provided is an empty PHONE companion app. Simply install Permanent Proxy on your Wear OS watch from the Play Store after installing this apk on your phone)

Sometimes big companies decide that Geoblocking is a good idea. I don't agree.
So that's why I created this Wear OS app to allow users to use their watch as if they were in another country. This means you can use certain payment apps or otherwise geospecific apps wherever you are in the world!

The app uses Android's built-in http_proxy command that turns on a proxy for the whole system and which normally can only be accessed using a computer and ADB Shell. This app however, uses the "ADB over Bluetooth" functionality of the watch to enable the proxy by itself. This can even be turned on at boot!

To get started, first enable the Developer Options of your watch, which can be achieved by going to Settings -> System -> About and tap the Build number until you are a "developer".
Next, go to Settings -> Developer options and enable "ADB debugging" and "Debug over Bluetooth".
Finally start up Permanent Proxy, request permission and press "Allow" or "Always allow this Computer" if prompted.
After requesting permission, ADB can be turned off again to save battery, unless you want to turn off the proxy completely.

Now you can get started!
Simply enter a proxy address and port, enable it (and on boot if you like) and you're done!

Proxy services can be found online and can be from any country. However, do make sure you completely trust the proxy you chose before you enter it! All the data of your watch might be sent through that proxy, even sensitive data, so act at your own risk.


Having trouble getting permission?

Some older watches are not powerful enough to get Secure Settings permission by itself using my method. If this is the case for your watch, you will need a PC to grant Permanent Proxy permission. The instructions are also available in the app description.

First make sure to connect your watch to your PC via ADB. You can Google how to do this, there are lots of tutorials. When your watch is connected, use the command

    adb shell pm grant nl.jolanrensen.permanentproxy android.permission.WRITE_SECURE_SETTINGS

to give Permanent Proxy the permissions needed to turn on/edit the proxy.

To turn off the proxy, connect to your PC in the same manner as before, but now you will need the following command:

    adb shell settings delete global http_proxy; adb shell settings delete global global_http_proxy_host; adb shell settings delete global global_http_proxy_port; adb shell settings delete global global_http_proxy_exclusion_list; adb shell settings delete global global_proxy_pac_url; adb shell reboot



Thanks to reddit user /u/shadowban!