# VescAlert
Android App providing configurable audio feedback alerts on the status of your Vedder Electronic Speed Controllers ( VESC )

Version 0.1 pre alpha.

Warning:
Currently meant as a companion app for other VESC status applications that open their own BT communication channel with a VESC.
There may be a standalone mode in the future.

You need to activate bluetooth logging in your phone's options as this works by "sniffing" the bluetooth traffic logged in sdcard/btsnoop_hci.log. 
To activate said option :
- Enable the developer options menu by clicking 7 times on the build n° in Settings > About phone > Build number
- Enable Bluetooth traffic logging in Settings > Developer options > Bluetooth HCI Snoop Log.

It is a good idea to delete your btsnoop_hci.log from time to time if it grows too large. You will then need to reset bluetooth connections before restarting VescAlert.

Only tested with Ackmaniac's VESC monitor and a 4.12 VESC / HM10 BT module at the moment. Send me your btsnoop_hci.log files for better support.
