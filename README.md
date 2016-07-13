# frog Milan Salone: Drone Race

Source code for the Drone Race game presented at frog Milan event for the Salone del Mobile 2016.
This is a racing and shooting game that is based on [Parrot JumpingSumo](http://www.parrot.com/usa/products/minidrones/jumping-race-drone/max/) drones and computer vision.

For a description of the project please check [this article](http://designmind.frogdesign.com/2016/07/hacking-a-multi-game-user-drone-race/)

The game is implemented by:
- An Android Mobile app that acts as remote controller, written mostly in Kotlin language, with parts in RenderScript/C and Java.
- A simple nodeJS based server component, acting as a broker to route messages among the game participants

To set up the race, it was also needed to connect drones to a central WiFi, thus changing
the standard configuration of the Parrot JumpingSumo drone Linux subsystem.
Notes to do that are available in the following.

This codebase was created to support a single event, as a hack journey in the fields of
robotics, computer vision, game design, hastily put together in our spare time.

That means a warning is due: bumpy code ahead - server side especially, code is
kind of messy. Client side, we tried to exploit the functional nature of Kotlin + RxJava,
to explore the potential of the technologies.


## JumpingSumo Drone Configuration

You'll need to login into the Drone IP (connecting to his own WiFi and IP)
and then perform the following:

Append at the end of `/etc/init.d/init_manager`

```
sleep 30 && sh /data/wifi.sh &
exec /etc/init.d/start_main_app
```

Then create a script called in `/data/wifi.sh` with contents:

```
killall udhcpd
ifconfig wifi_bcm down
iwconfig wifi_bcm mode managed essid <you AP SSSID>
ifconfig wifi_bcm <IP to be assigned ot drone> netmask 255.255.255.0 up
```

Please also note that the drones cannot acts as DHCP clients, so you need to
assign static ip. Your WiFi should also be devoid of any authentication since
the stripped-down Linux that is on-board of Parrots cannot handle that.
It is also suggested to use a 5ghz Wifi setting and a free channel.

To finish set up, edit the file `dragon.conf` as following

```
{
        "JumpingSumo" :
        {
                "audio_theme" : 0,
                "auto_record" : 0,
                "volume" : 0.0, //stop all those noise from the drone
                "wifi_autoselect_mode" : "none",
                "wifi_band" : 1, //0 is for 2.4 ghz, 1 for 5 ghz
                "wifi_channel" : <wifi channel chosen>,
                "wifi_settings_outdoor" : false
        },
        "network" :
        {
                "auto_country" : 0,
                "country_code" : "<two letter country code>",
                "default_c2dport" : 54321,
                "default_d2cport" : 43210,
                "product_name" : "<drone name>",
                "service_type" : "_arsdk-0902._udp"
        }
}
```

Reboot the drone from the shell and you should be able to see it connecting to your
WiFi in a couple of minutes.
