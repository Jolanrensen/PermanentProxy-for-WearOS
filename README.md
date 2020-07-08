# PermanentProxy for Wear OS
Simple app to run a proxy (on boot) on a Wear OS device and enable geo-restricted apps like Google Pay in unsupported regions!

Sometimes big companies decide that Geoblocking is a good idea. I don't agree.
So that's why I created this Wear OS app to allow users to use their watch as if they were in another country. This means you can use certain payment apps or otherwise geospecific apps wherever you are in the world!

The app uses Android's built-in http_proxy command that turns on a proxy for the whole system and which normally can only be accessed using a computer and ADB Shell. This app however, uses the "ADB over Bluetooth" functionality of the watch to enable the proxy by itself. This can even be turned on at boot!

# Installation
Method 1:
Download the latest mobile-release.apk from https://github.com/Jolanrensen/PermanentProxy-for-WearOS/releases or from https://labs.xda-developers.com/store/app/nl.jolanrensen.permanentproxy and install it on your Android phone.
Next, go to the "Apps on your phone" section in the Play Store on your Wear OS watch.
Install Permanent Proxy.

Method 2:
Download the latest wear-release.apk from https://github.com/Jolanrensen/PermanentProxy-for-WearOS/releases. Install it on your Wear OS watch directly using ADB.

# Get started
To get started, first enable the Developer Options of your watch, which can be achieved by going to Settings -> System -> About and tap the Build number until you are a "developer".
Next, go to Settings -> Developer options and enable "ADB debugging" and "Debug over Bluetooth".
Finally start up Permanent Proxy, request permission and press "Allow" or "Always allow this Computer" if prompted.
After requesting permission, ADB can be turned off again to save battery, unless you want to turn off the proxy completely.

Now you can get started!
Simply enter a proxy address and port, enable it (and on boot if you like) and you're done!

Proxy services can be found online and can be from any country. However, do make sure you completely trust the proxy you chose before you enter it! All the data of your watch might be sent through that proxy, even sensitive data, so act at your own risk.


# Having trouble getting permission?

Some older watches are not powerful enough to get Secure Settings permission by itself using my method. If this is the case for your watch, you will need a PC to grant Permanent Proxy permission. The instructions are also available in the app description.

First make sure to connect your watch to your PC via ADB. You can Google how to do this, there are lots of tutorials. When your watch is connected, use the command

    adb shell pm grant nl.jolanrensen.permanentproxy android.permission.WRITE_SECURE_SETTINGS

to give Permanent Proxy the permissions needed to turn on/edit the proxy.

To turn off the proxy, connect to your PC in the same manner as before, but now you will need the following command:

    adb shell settings delete global http_proxy; adb shell settings delete global global_http_proxy_host; adb shell settings delete global global_http_proxy_port; adb shell settings delete global global_http_proxy_exclusion_list; adb shell settings delete global global_proxy_pac_url; adb shell reboot

# Some tips:
- Create your own proxy. Free proxies might work, but usually they stop working within a couple of days if they work at all. A tutorial can be found on XDA developers.
- Use an obscure port on your proxy since all IPv4s are regularly scanned for common services, a passwordless proxy will stand out like a beacon (see https://www.shodan.io/search?query=squid for a couple of million http-proxies). 
- Proxies with a password don't work.
- Don't sideload Google Pay on your watch. It should enable by itself. You can try to disable/enable Google Pay from the Play Store on your watch by searching for it, or by using the Pay Enabler app to quickly open the Google Pay page in the Play Store.
- Clearing the data of Google Pay using the settings on the watch also helps sometimes. You can also find remove updates / disable the app there.
- After enabling the proxy, exit the app, give your watch some time and then check back in the Permanent Proxy app to see whether your External IP has changed to the one you entered as proxy. If so, good job, you're connected!
- You can make your watch refresh its Google Pay availability check in two ways. Either by setting the IP/port again in the Permanent Proxy app, or by tapping "Home App" in Settings, System, About, Versions.
- Google Pay works if you see the icon in your quick settings.
- Google Pay get's stuck when adding a new card? Check the Google Play stores for updates for Google Pay.
- Only cards you can use to pay with your phone in stores can be added to Google Pay on your watch. This means you'll need a card from a supported Google Pay country.
- Permanent Proxy only works for IPv4 networks. If you're on a network that uses IPv6, in theory, Google can still check your country. Turn off WiFi if you have to.
- Using a VPN on your phone that routs the internet traffic of the Wear OS app through it is an alternative to Permanent Proxy, but you'll have to turn off WiFi on your watch, only use Bluetooth and keep the VPN on your phone always on.
- Uninstalling Permanent Proxy will not stop the proxy. Stop the proxy using the app before uninstalling it, or stop the proxy using the ADB command above.


Thanks to reddit user /u/shadowban!
