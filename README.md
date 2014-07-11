CosyDVR
=======

android open souce car blackbox DVR app (GPLv3+)

Features:
- Free and open source project forever
- Works in background as a service
- Works even when the keyboard is locked
- Autostart recording option after program launch
- Autoremove old files
- Simple placing files to favorite folder (one file or all files after current)
- FLASH button for night recordings (if device supports)
- Supports Android 4.1.2 (Fly IQ451 primarily tested)
- GPS data is recorded into subtitle files *.srt
- Separate *.gpx file for upload to the OpenStreetMap.org
- GPS data is shown on the sceen
- Night mode for better videon under low light condition (if device supports)
- Different focus modes (infinity, auto, continuous video, macro)
- Zoooming in/out  with gestures
- Refocus on screen tap
- Exit function is protected with long click to avoid accedental click
- Configurable options (video size, bitrate, maxtemp and minfree space, fragment time etc.)
- Notification area clickable icon for bringing app to front
- Custom SD_Card_Path option
- Ukrainian Russian English interfaces
- Show battery level when device is not charging

Supported devices:
- Fly IQ451
- Prestigio PAP4055
- ASUS ME173X
- Samsung SM-GT310

Not yet supported devices:

Features todo:
Add to favorites previous file if it is finished less than minute ago.
Add auto call answer ability (maybe not needed any more)
Add auto night mode on option

Operation manual:
Buttons:
FAV - preserve current fragment from deletion (add to favorites).
        0 - this is temp fragment
        1 - this and all next fragments are preserved
        2 - only this fragment is preserved. Next will be temp
Restart - break fragments. Start new one
Restart (long click) - options menu
Focus - switch between focus modes (visible only supported by device)
        i - infinity
        v - continuous video
        a - automatic with manual autofocus
        m - macro with manual autofocus
        e - extended depth of field
FLASH - toggle flash on/off
Night - toggle night mode on/off
Exit - exit application (exit on long click for eliminate accidental press)

Gestures:
Tap on screen - autofocus in "aut" and "mac" modes
Pinch screen - zoom in/out

License:
        This program is Free Software: You can use, study share and improve it at your will. 
        Specifically you can redistribute and/or modify it under the terms of the 
        GNU General Public License as published by the Free Software Foundation, 
        either version 3 of the License, or (at your option) any later version.
