#Bucanero: 2nd Gen OG1 Box App


##Architecture
Bucanero is a major simplifcation over AmstelBright, the previous implementation. In AB, all apps were served from the OG1 box to the webviews being displayed on screen,
and on the handset. This in case of internet connections failures. During the AB alpha trials, we learned that
even unsophisitcated venues may be using firewalling on their public WiFi, effectively blocking communication
between peers (i.e. the handset talking to the OG1).

To solve this, we switched to using the cloud as the main control channel. Handsets now talk to the cloud (Bellini Device Manager) and
messages are forwarded to the target OG1 and vice versa. This ended up removing a lot of complexity from AB and gave us
the ability to do upgrades much quicker, since we did not need to release new software to the in-venue boxes.

###Activities

There are only two Activities in Bucanero: 
- MainFrame: runs the webviews, displays messages, controls the TV display. MainFrame is the public TV face of Ourglass :).
- PermissionsGate: this activity asks for specific required permissions the first time Bucanero runs. Ultimately this Activity will go away when we 
release Bucanero as a system application.

A third App, called Wort, will run before Bucanero and check our servers for Bucanero upgrades when the system
starts, or when run by a user.

###Services and Service-Like Objects

There are several "real" Android Services and a few objects that act like services but do not inherit from Service. The choice of implementing
a full service or not was *not* based on hard criteria...

- **FFMpegBinaryService:** handles sampling the audio from HDMI-RX and streaming it to the Websockets audio server.
- **ConnectivityCenter:** handles monitoring WiFi and Ethernet connectivity.
- **OGLogService:** periodically uploads logcats to Bellini-DM and handles posting of log messages.
- **SocketIOManager:** handles SocketIO communication with BelliniDM.
- **STBPollingService:** monitors the set top box polling for DirecTV

###BOOTUP

Bucanero starts up in a sub-class of Application called `ABApplication.js` which does the following.

During object instantiation or in `onCreate()`:

- Creates a shared, static `MainThreadBus` for sending Square Otto bus messages that always resolve on the main thread.
- Creates a shared, static `OkHttpClient` for use by the entire app. Square strongly recommends using a singleton client for the whole app, so after having lots of problems, we agreed :D.
- Creates a shared, static Context for use throughout the app.
- Sets up Realm for use throughout the app. Realm is used for persistent storage of Log messages.
- Creates an instance of `ConnectivityCenter` and starts the process to link with the cloud.
- Globally initializes `JodaTime` 

The `PermissionGate` Activity is the first one loaded and it checks to see if all required permissions
have been granted (which is usually the case after first setup). If we're cool, PermissionGate barely flickers on 
screen and then it calls `ABApplication.boot()` and transfers control to `MainFrameActivity`.

`ABApplication.boot()` does the following:
- Decides (based on a setting in `OGConstants`) whether to run Fast Boot or Slow Boot. Slow boot just slows the process down enough for
messages to be readable on-screen. This is the default.
- In a seperate Thread runs:
    - ~~Brings up the Ethernet port to a static IP address for hard-pair to the STB.~~ This is now in Wort.
    - Starts all true services:
        - OGLogService
        - STBPollingService
        - FFmpegBinaryService
        

###Networking Operation

Networking boots up as follows:
- ABApplication calls `ConnectivityCenter.getInstance()` which does the following:
    - Creates a singleton
    - Calls `init()` which:
        - gets a handle to the Android ConnectivityManager's CONNECTIVITY_SERVICE
        - gets the WiFi network info via the (deprecated) `getNetworkInfo` call
        - gets the Ethernet network info thru the same call
        - registers a Broadcast Receiver for when ConnectivityManager tosses off network change
          events. The receiver calls `refreshNetworkState()`
        - Calls `refreshNetworkState()` which:
            - Checks if the WiFi is connected
            - Checks if the Ethernet is connected
            - Grabs the WiFi IP address
            - DOES NOT toss off an Otto message (it probably should).
    - On the singleton, `ABApplication` calls `initializeCloudComms(WITHOUT CALLBACK)` which:
        - Authenticates as an admin (this needs to change STAT)
            - If this works:
                - it sets `isCloudDMReachable` to TRUE
                - Grabs the session cookie
                - Fires off the following in a mHandler post:
                    - creation of `SocketIOManager`, or reset of the `SocketIOManager`s cookie.
                    - Registers the device with the cloud
                        - Success: callback that bringup is done.
                        - Fail: callback is executed with error
                - Outside of the above mHandler post, issues a message that the network is connected. This is definitely WRONG.
            - If the authenticate FAILS:
                - Issues a NETWORK_LOS Otto message
                - Waits 60 seconds and tries again
                - This is also wonky
        
                
### More Networking Code

##UDHCPD Configuration File

This file gets written in `ABApplication`. It's stored in `raw` assets.


##Device UDID

The UDID is saved to the sdcard root on first install, that way if Buc is uninstalled, it will find the original UDID and use it.
The only way to generate a new UDID is to manually erase the file `udid.txt` in the root of the SDCARD. This is by design.
