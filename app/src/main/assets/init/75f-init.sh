#!/system/bin/sh

VERSION="1.0b"   # Do not change this tag format, the code searches for this
TAG="75f-init"

WHOAMI=`id`
log -p v -t $TAG "75f-init version $VERSION running as $WHOAMI"

CURRENT_LAUNCHER=`cmd shortcut get-default-launcher`
log -p v -t "$TAG" "Current launcher is $CURRENT_LAUNCHER"

# *******************************************
# * Wait for package manager to start
# *******************************************
log -p v -t "$TAG" "Checking for package manager"
service list | grep package
STATUS=$?
while [ $STATUS -ne 0 ]
do
  log -p v -t "$TAG" "Package manager not running, waiting for package manager"
  sleep 10
  service list | grep package
  STATUS=$?
done

log -p v -t "$TAG" "Package manager has started"

# *******************************************
# * Grab current meta data
# *******************************************
RESULTS=`dumpsys package com.x75frenatus.home| grep -e codePath -e versionName -e timestamp -e firstInstall -e lastUpdate | sort -u`
log -p v -t "$TAG" "Current home app installation"
log -p v -t "$TAG" "$RESULTS"

CURRENT_INSTALLATION=`dumpsys package com.x75frenatus.home| grep codePath | grep apk | sed 's/^ *codePath=//'`
LEGACY_HOME_APP=`ls -1 /system/priv-app/*.apk 2>/dev/null| grep 75fHome | head -n 1`
HOME_APP=`ls -1 /system/priv-app/*.apk 2>/dev/null| grep HomeApp | head -n 1`

log -p v -t "$TAG" "Legacy home apk: [$LEGACY_HOME_APP]"
log -p v -t "$TAG" "Home apk: [$HOME_APP]"
log -p v -t "$TAG" "Current installation: [$CURRENT_INSTALLATION]"

# *******************************************
# * Install the HOME_APP package if present
# *******************************************
if [ "$HOME_APP" != "" ]
then
  log -p v -t "$TAG" "Found home app apk $HOME_APP"

  # ****************************************************************
  # * If the apk in priv-app is different than what is installed
  # ****************************************************************
  if [ "$HOME_APP" != "$CURRENT_INSTALLATION" ]
  then
    # *******************************************
    # * Get rid of the old Home App
    # *******************************************
    if [ "$LEGACY_HOME_APP" != "" ]
    then
      log -p v -t "$TAG" "Uninstalling legacy home app apk $LEGACY_HOME_APP"

      # Make the file system writable so that we can remove the file
      mount -o rw,remount /system

      RESULT=`/system/bin/pm uninstall --user 0 com.x75frenatus.home 2>&1`
      STATUS=$?
      log -p v -t "$TAG" "Status $STATUS Resulting output: $RESULT"
      log -p v -t "$TAG" "Removing old APK"
      rm -f $LEGACY_HOME_APP

      # Restore the file system to read only
      mount -o ro,remount /system
    fi

    # *******************************************************************************
    # * Perform the actual install
    # * We do not know, at this point, if a previous installation succeeded or failed
    # *******************************************************************************
    log -p v -t "$TAG" "Installing HomeApp $HOME_APP"
    chmod 644 $HOME_APP
    chown root.root $HOME_APP
    RESULT=`pm install -r --user 0 $HOME_APP 2>&1`
    STATUS=$?
    log -p v -t "$TAG" "Status $STATUS Resulting output: $RESULT"
  else
    log -p v -t "$TAG" "Home app version has already been installed"
  fi
fi

# *****************************************************************************************
# * If we have a home app package installed in the system, use it as the default launcher
# *****************************************************************************************
log -p v -t "$TAG" "Setting default launcher/home activity"
RESULT=`/system/bin/cmd package set-home-activity --user 0 com.x75frenatus.home/.MainActivity 2>&1`
STATUS=$?
log -p v -t "$TAG" "Status $STATUS Resulting output: $RESULT"

log -p v -t "$TAG" "Starting 75f launcher"
RESULT=`/system/bin/sh /system/bin/am start -n com.x75frenatus.home/.MainActivity 2>&1`
STATUS=$?
log -p v -t "$TAG" "Status $STATUS Resulting output: $RESULT"

# ************************************************************
# * For sanity, double check and report the default launcher
# ************************************************************
RESULTS=`dumpsys package com.x75frenatus.home| grep -e codePath -e versionName -e timestamp -e firstInstall -e lastUpdate | sort -u`
log -p v -t "$TAG" "Final home app installation"
log -p v -t "$TAG" "$RESULTS"

CURRENT_LAUNCHER=`cmd shortcut get-default-launcher`
log -p v -t "$TAG" "75f-init complete, current launcher is $CURRENT_LAUNCHER"