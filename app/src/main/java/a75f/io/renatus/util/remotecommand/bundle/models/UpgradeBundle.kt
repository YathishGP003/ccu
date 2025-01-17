package a75f.io.renatus.util.remotecommand.bundle.models

/**
 * UpgradeBundle is returned by the BundleInstallManager.getRecommendedUpgradeBundle and used
 * to communicate available recommended bundle information to the CCU UI.
 */
data class UpgradeBundle(val bundle: BundleDTO) {
    val errorMessages = mutableListOf<String>()     // Error messages
    val upgradeOkay: Boolean                        // True if okay to upgrade to bundle
        get() = errorMessages.isEmpty()

    val componentsToUpgrade: MutableList<ArtifactDTO> = mutableListOf()

    fun addErrorMessage(message: String) {
        errorMessages.add(message)
    }
}