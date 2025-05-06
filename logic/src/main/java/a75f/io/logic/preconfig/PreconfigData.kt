package a75f.io.logic.preconfig


data class PreconfigurationData (
    val sitePreConfigId : String,
    val siteName : String,
    val ccuName : String,
    val orgName : String,
    val timeZone : String,
    val siteAddress : SiteAddress,
    val fmEmail: String,
    val installerEmail: String,

    val systemProfile : String,
    val relayMappingSet : List<String>,

    val floor : String,
    val zones : List<String>,
    val pizzaType : String,
    val createdBy : User,
    val activationCode : String,
    val createdAt : String,
    val expiresAt : String,
    val activationCodeCreatedBy : User
)

data class SiteAddress (
    val geoCity : String,
    val geoState : String,
    val geoCountry : String,
    val geoAddr : String,
    val geoPostalCode : String
) {
    fun getFormattedAddress(): String {
        return "$geoAddr, $geoCity, $geoState, $geoCountry, $geoPostalCode"
    }
}

data class User (
    val userId : String,
    val firstName : String,
    val lastName : String,
    val emailAddress : String
)
