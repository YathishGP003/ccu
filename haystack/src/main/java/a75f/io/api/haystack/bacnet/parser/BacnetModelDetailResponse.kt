package a75f.io.api.haystack.bacnet.parser

import a75f.io.api.haystack.Tags
import com.google.gson.annotations.SerializedName
import android.util.Pair

data class BacnetModelDetailResponse(
    @SerializedName("id") var id: String,
    @SerializedName("domainName") val domainName: String,
    @SerializedName("name") var name: String,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("description") val description: String? = null, // added
    @SerializedName("modelType") var modelType: String? = null,
    @SerializedName("tagNames") var equipTagNames: MutableList<String>,
    @SerializedName("tags") var equipTagsList: MutableList<TagItem>,
    @SerializedName("points") var points: MutableList<BacnetPoint>,
    @SerializedName("bacnetConfig") var bacnetConfig: String? = null,
    @SerializedName("modelConfig") var modelConfig: String? = null,
) {
    constructor() : this("","", "", "", "", "", mutableListOf(), mutableListOf(), mutableListOf())
}

data class BacnetZoneViewItem(
    val disName: String,
    val value: String,
    val bacnetConfig: String,
    val isVisibleOnUi: Boolean,
    val bacnetObj: BacnetPoint,
    val isWritable: Boolean,
    val spinnerValues: MutableList<Pair<String,Int>>,
    val objectType : String
)

data class BacnetPoint(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("domainName") val domainName: String,
    @SerializedName("kind") val kind: String,
    @SerializedName("valueConstraint") val valueConstraint: ValueConstraint?,
    @SerializedName("presentationData") val presentationData: PresentationData?,
    @SerializedName("hisInterpolate") val hisInterpolate: String,
    @SerializedName("protocolData") val protocolData: ProtocolData?,
    @SerializedName("defaultUnit") val defaultUnit: String,
    @SerializedName("defaultValue") val defaultValue: String,
    @SerializedName("tagNames") var equipTagNames: MutableList<String>,
    @SerializedName("rootTagNames") var rootTagNames: MutableList<String>,
    @SerializedName("descriptiveTags") var descriptiveTags: MutableList<TagItem>,
    @SerializedName("tags") var equipTagsList: MutableList<TagItem>,
    @SerializedName("bacnetProperties") var bacnetProperties: MutableList<BacnetProperty>,
    @SerializedName("point") val point : DataPoint?= null,
    // used to show and hide using arrow
    var displayInEditor: Boolean = true,
    // used to show and hide point in zone view
    var displayInUi: Boolean = true,
    @SerializedName("disName") val disName: String = "",
    @SerializedName("defaultWriteLevel") var defaultWriteLevel: String = "8",
    @SerializedName("isSystem") var isSystem : Boolean = false,
    var isSchedulable: Boolean = false,
    @SerializedName("bacnetObjectId") var bacnetObjectId: Int? = 0
){
    fun initailizeSchedulableForFreshPairing() {
        isSchedulable = equipTagNames.contains(Tags.WRITABLE) && equipTagNames.contains(Tags.SCHEDULABLE)
    }

    constructor() : this("", "", "", "", null,null, "", null, "", ""
        , mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf(), null,true, true, "", isSchedulable = false, bacnetObjectId = 0)
}

data class PresentationData(
    @SerializedName("tagValueIncrement") val tagValueIncrement: String?
)
data class DataPoint(
    @SerializedName("name") val name: String,
    @SerializedName("tags") var equipTagsList: MutableList<TagItem>,
    @SerializedName("valueConstraint") val valueConstraint: ValueConstraint?,
    @SerializedName("presentationData") val presentationData: PresentationData?,
    @SerializedName("defaultUnit") val defaultUnit: String,

    )

data class ValueConstraint(
    @SerializedName("constraintType") val constraintType: String,
    @SerializedName("minValue") val minValue: Double?,
    @SerializedName("maxValue") val maxValue: Double?,
    @SerializedName("allowedValues") val allowedValues: MutableList<AllowedValues>?,

    //@SerializedName("allowedValues") val unit: MutableList<AllowedValues>?,
)

data class AllowedValues(
    @SerializedName("index") val index: Int?,
    @SerializedName("value") val value: String?,
    @SerializedName("dis") val dis: String?,
)

data class TagItem(
    @SerializedName("name") val name: String,
    @SerializedName("kind") val kind: String,
    @SerializedName("defaultValue") val defaultValue: Any?,
    @SerializedName("valueEnum") val valueEnum: MutableList<String>?
)

data class BacnetProperty(
    @SerializedName("name") val name: String,
    @SerializedName("id") val id: Int,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("dataType") val dataType: String? = null,
    @SerializedName("defaultValue") val defaultValue: Any? = null,
    @SerializedName("permission") val permission: String,
    @SerializedName("fetchedValue") var fetchedValue: String? = null,
    @SerializedName("selectedValue") var selectedValue: Int = 0,
    @SerializedName("propertyIndex") var propertyIndex: Int = 0,
)

enum class BacnetSelectedValue {
    DEVICE,
    FETCHED
}

data class BacnetProtocolData(
    @SerializedName("objectType") val objectType: String?,
    @SerializedName("objectId") val objectId: Int?,
    @SerializedName("protocolType") val protocolType: String?,
    @SerializedName("displayInUIDefault") var displayInUIDefault: Boolean,
    @SerializedName("properties") val properties: MutableList<String>?,
)

data class ProtocolData(
    @SerializedName("BACNET") val bacnet: BacnetProtocolData,
)
