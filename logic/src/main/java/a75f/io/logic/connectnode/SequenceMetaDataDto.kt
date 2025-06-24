package a75f.io.logic.connectnode

data class SequenceMetaDataDTO(
    val seqId: String,
    val seqName: String,
    val metadata: List<ModelMetadata>,
    val lowCode: String?,
    val version: Int,
    val signature : String,
    val sizeInBytes : Int
)

data class ModelMetadata(
    val modelId: String,
    val modelName: String,
    val modelVersion: String,
    val points: List<PointData>,
    val suffix : String?,
)

data class PointData(
    val pointId: String,
    val pointName: String,
    val registerAddress: String
)
