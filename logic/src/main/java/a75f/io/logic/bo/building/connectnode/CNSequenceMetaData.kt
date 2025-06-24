package a75f.io.logic.bo.building.connectnode

data class CNSequenceMetaData(
    val firmwareSignature: String,
    val sequenceName: String,
    val version: Int,
    val deviceType: Int,
    val updateLength: Int
)
