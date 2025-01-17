package a75f.io.renatus.util.remotecommand.bundle.models

/**
 * AppUpgradeInfo is used to communicate the state of the various upgradable components to the CCU UI.
 *
 * This data class addresses the need for a number of different upgrade scenarios:
 * Note: The values below will be provided for each component, however we've kept it brief for
 *       illustration purposes.
 *
 * Scenario 1: A valid upgrade is available for all components
 *   AppVersion    - 2.18.1 - The version that the component will be updated to
 *   UpdateState   - UPDATABLE - Indicates that the component can be upgraded to the specified version
 *   UpdateMessage - In this case, not very useful, but will be populated with something like
 *                  "Component can be updated"
 *
 * Scenario 2: 1 or more components are at a version >= the version specified in the bundle.
 * In this case, the bundle is still valid, however those components will not be updated.  In this
 * case we will assume that the RemoteAppVersion was upgraded to 0.1.36 manually prior to checking
 * the recommended bundle.
 *
 *   AppVersion       - 0.1.36 - The version that a component would normally be upgraded to (e.g. 0.1.36)
 *   AppUpdateState   - CURRENT - Indicates that this component does not require an upgrade at
 *                                this time
 *   AppUpdateMessage - "RemoteApp is already at version 0.1.36 and does not require upgrade"
 *
 * Scenario 3: 1 or more components are not installed
 * It is a requirement that, for a bundle to be installed, all components listed have already been
 * installed on the CCU.  In this case, we will assume that the recommended bundle includes
 * BACApp version 2.0.0, however the CCU does not have BACApp installed.
 *
 *   AppVersion       - 2.0.0
 *   AppUpdateState   - MANUAL_UPDATE_REQUIRED
 *   AppUpdateMessage - "BACApp is not currently installed.  A manual install must occur before
 *                          the bundle can be installed."
 *
 * Scenario 4: 1 or more components are not part of the bundle
 * It may be the case that certain components are not included as part of a bundle.  In this case,
 * those components are simply ignored.  In this scenario, it is assumed that BACApp is not a part
 * of the bundle.
 *
 *   AppVersion       - null
 *   AppUpdateState   - NOT_IN_BUNDLE
 *   AppUpdateMessage - "BACApp is not in the bundle and will not be installed."
 *
 * Scenario 5: 1 or more components do not meet the minimum version requirement
 * It is a requirement that, for a bundle to be installed, all components listed meet the minimum
 * version requirement.  In this case, we will assume that the recommended bundle includes
 * CCUApp version 2.0.0, however the bundle requires a minimum version of 2.17.1
 *
 *   AppVersion       - 2.17.3
 *   AppUpdateState   - MANUAL_UPDATE_REQUIRED
 *   AppUpdateMessage - "The CCUApp does not meet the minimum version requirement of 2.17.1.
 *                          A manual upgrade must occur before the bundle can be installed."
 *
 * Full example including a number of failure scenarios combined.
 *
 *    bundleId: "some-version-identifier",
 *    bundleName: "Invictus 2.18.1",
 *    upgradeOkay: false,
 *    CCUAppVersion: "2.18.1",
 *    CCUAppState: UPDATABLE,
 *    CCUAppUpdateMessage: "CCUApp will be upgraded",
 *    BACAppVersion: null,
 *    BACAppState: NOT_IN_BUNDLE,
 *    BACAppUpdateMessage: "BACApp is not in the bundle and will not be installed",
 *    RemoteAppVersion: 0.1.36,
 *    RemoteAppUpdateState: UPDATABLE,
 *    RemoteAppUpdateMessage: "CCUApp will be upgraded",
 *    HomeAppVersion: 4.1.22,
 *    HomeAppUpdateState: MANUAL_UPDATE_REQUIRED,
 *    HomeAppUpdateMessage: "The HomeApp does not meet the minimum version requirement of 4.1.1. A manual upgrade must occur before the bundle can be installed.",
 *
 *
 */
data class AppUpgradeInfo(val appName: String,
                          val appFilename: String?,
                          val appUpdateState: ComponentUpdateState,
                          val updateMessage: String,
) {
    enum class ComponentUpdateState {
        // The following 3 component states are still valid bundle install states
        UPDATABLE,                      // Updatable as part of the bundle OTA
        NOT_IN_BUNDLE,                  // Component is not part of the bundle
        CURRENT,                        // This component version is >= the version specified by the bundle and does not require an update

        // The following state essentially disqualifies a bundle from being installed
        MANUAL_UPDATE_REQUIRED,         // A manual update of this component is required before the bundle can be installed
    }

    val updateFilename: String? = null
}