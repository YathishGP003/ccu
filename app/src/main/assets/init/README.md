These scripts integrate with the Android boot initialization system and are compatible with the CCU50
and XR tablets.  As part of the android startup, when boot is complete the init-75f.rc causes
the 75f-init.sh shell script to be executed one time.  It's purpose is to complete and/or fix any
HomeApp OTA installation issues that may occur.

init-75f.rc - The actual hook into the init system, calls the shell script /system/bin/75f-init.sh

75f-init.sh - Ensures that the HomeApp apk has been installed, that the default launcher is set, and starts the HomeApp

These scripts must be deployed to the following locations on the tablet:

init-75f.rc -> /system/etc/init
75f-init.sh -> /system/bin

After installation, the following permission must be set:

chmod 755 /system/bin/75f-init.sh

Notes:
For questions or more details, reach out to Bill Johnson, Sam Sadasivan, or Luke Gaskill

See ADR-0105 for a more complete discussion on the init system
https://dev.azure.com/75fdevelopment/75F%20Engineering/_wiki/wikis/75F-Engineering.wiki/3537/ADR-0105-CCU-system-initialization-and-monitoring-at-tablet-boot-time

