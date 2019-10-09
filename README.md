# PermanentProxy-for-WearOS
App that will enable (on boot) a defined proxy on Wear OS watches. Could be used to enable Google Pay in "unsupported countries"

Sometimes big companies decide that Geoblocking is a good idea. I don't agree.
So that's why I created this Wear OS app to allow users to use their watch as if they were in another country. This means you can use certain payment apps or otherwise geospecific apps wherever you are in the world!

The app uses Android's built-in http_proxy command that turns on a proxy for the whole system and which normally can only be accessed using a computer and ADB Shell. This app however, uses the "ADB over Bluetooth" functionality of the watch to enable the proxy by itself. This can even be turned on at boot!

To get started, first enable the Developer Options of your watch, which can be achieved by going to Settings -> System -> About and tap the Build number until you are a "developer".
Next, go to Settings -> Developer options and enable "ADB debugging" and "Debug over Bluetooth".
Finally start up Permanent Proxy and press "Always allow this Computer" if prompted.
Now you can get started!
Simply enter a proxy address and port, enable it (and on boot if you like) and you're done!

Proxy services can be found online and can be from any country. However, do make sure you completely trust the proxy you chose before you enter it! All the data of your watch might be sent through that proxy, even sensitive data, so act at your own risk.
