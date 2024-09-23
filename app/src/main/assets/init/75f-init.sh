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
HOME_APP=`ls -1 /system/priv-app/*.apk 2>/dev/null| grep HomeApp | head -n 1`

log -p v -t "$TAG" "Home apk: [$HOME_APP]"
log -p v -t "$TAG" "Current installation: [$CURRENT_INSTALLATION]"

# *******************************************
# * Install the HOME_APP package if present
# *******************************************
if [ "$HOME_APP" != "" ]
then
  log -p v -t "$TAG" "Found home app apk $HOME_APP"

  # ******************************************************************
  # * We have a home app with the latest naming convention, uninstall
  # * old home app versions beginning with 'HomeRenatus' or '75fHome'.
  # * Generally we'll either have 0 or 1 here.
  # ******************************************************************
  LEGACY_FILES=`ls /system/priv-app/*.apk 2>/dev/null| grep -i -e 75fHome -e HomeRenatus`
  if [ $? -eq 0 ]
  then
    # Make the file system writable so that we can remove the file
    OUTPUT=`mount -o rw,remount /system 2>&1`
    if [ $? -ne 0 ]
    then
      log -p v -t "$TAG" "Re-mount failed, unexpected behavior may occur: $OUTPUT"
    fi

    OUTPUT=`mount | grep "/system "`
    log -p v -t "$TAG" "Mount state: $OUTPUT"

    log -p v -t "$TAG" "Uninstalling legacy home app com.x75frenatus.home"

    RESULT=`/system/bin/pm uninstall --user 0 com.x75frenatus.home 2>&1`
    STATUS=$?
    log -p v -t "$TAG" "Status $STATUS Resulting output: $RESULT"

    # ******************************************************************
    # * Remove each of the found APKs
    # ******************************************************************
    for LEGACY_HOME_APP in $LEGACY_FILES
    do
      log -p v -t "$TAG" "Removing old APK $LEGACY_HOME_APP"
      rm -f $LEGACY_HOME_APP
    done

      # Restore the file system to read only
      mount -o ro,remount /system
  fi

  # ****************************************************************
  # * If the apk in priv-app is different than what is installed
  # ****************************************************************
  if [ "$HOME_APP" != "$CURRENT_INSTALLATION" ]
  then
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
else
  log -p v -t "$TAG" "CCU tablet is still running the legacy home app"
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