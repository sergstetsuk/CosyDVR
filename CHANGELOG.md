### v.1.3.9 (2015.06.12 22:12)

  * Trying to fix KitKat SDcard issue. Now video is saved to [SDcard]/Android/data/es.esy.CosyDVR/fav or temp
  * Fixed preview orientation when there are no user defined preferences at all.

### v.1.3.8 (2015.06.10 19:30)

  * Refactored scanning storage resources

### v.1.3.7 (2015.05.31 09:45)

  * Fixed crash for devices without flash.

### v.1.3.6 (2015.05.12 20:04)

  * Added reverse landscape orientation option in preferences (needs to reinstall app or clear defaults)
    Reverse landscape mode is applied after application restart.

### v.1.3.5 (2015.05.11 20:04)

  * Added Italian translation thanks to fvasco
  * Renamed package to es.esy.CosyDVR

### v.1.3.4 (2015.04.21 16:29)

  * Added propagation of zoomfactor through fragments (Juan Jose Zamorano issue)

### v.1.3.3 ()

  * Fixed russian translation in order to implement Dmitry Lahoda patch.
  
### v.1.3.2 (2014.11.20 18:20)

  * Added preference to disable GPS usage

### v.1.3.1 (2014.11.18 11:15)

  * Fixed preferences english texts, added units in pref. dialogs on video parameters and so on
  * merged push request on README.md from asd-and-Rizzo

### v.1.3 (2014.11.13 20:11)

  * Fixed propagation of night mode, flash status and focus mode between fragments

### v.1.2 (2014.07.11 11:08)

  * Fixed autoclean temp folder
  * Added battery level indicator when device is not charging

### v.1.1 (2014.06.26 16:08)

  * Removed unnecessary permissions and features from AndroidManifest.xml
  * Added Help and About in preferences
  * Added Ukrainian interface
  * Added Russian interface

### v.1.0.6 (2014.06.25 16:17)

  * Added update FAV button during fragment split (to see if fav mode is on)

### v.1.0.5 (2014.06.24 11:50)

  * External SD_Card property now is list of available devices
  * Fixed camera initialization for Samsung SM-GT310
  * Fixed CosyDVR dir creation for Samsung SM-GT310
  * Added checking focus modes after start
  * Added checking supported scene modes (night/auto)

### v.1.0.4 (2014.06.23 15:12)

  * Fixed "Video Height" property

### v.1.0.3 (2014.06.20 17:25)

  * Fixed "Autostart Recording" property
  * Fixed "SD_Card_Path" property usage after program start

### v.1.0.2 (2014-06-13)

  * Added "Autostart Recording" property

### v.1.0.1 (2014-06-12)

  * Added preference "SD card destination path"
  * Simplified focus modes names (for devices with small screen)

### v.1.0 (2014-05-22)

  * First public version for inclusion to F-Droid repository
