package a75f.io.renatus.bacnet.models

import a75f.io.api.haystack.bacnet.parser.BacnetProperty
import a75f.io.api.haystack.bacnet.parser.ProtocolData
import a75f.io.api.haystack.bacnet.parser.TagItem
import a75f.io.api.haystack.bacnet.parser.ValueConstraint
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.google.gson.annotations.SerializedName

data class BacnetPointState(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("domainName") val domainName: String,
    @SerializedName("kind") val kind: String,
    @SerializedName("valueConstraint") val valueConstraint: ValueConstraint?,
    @SerializedName("hisInterpolate") val hisInterpolate: String,
    @SerializedName("protocolData") val protocolData: ProtocolData?,
    @SerializedName("defaultUnit") val defaultUnit: String,
    @SerializedName("defaultValue") val defaultValue: String?,
    @SerializedName("tagNames") var equipTagNames: MutableList<String>,
    @SerializedName("rootTagNames") var rootTagNames: MutableList<String>,
    @SerializedName("descriptiveTags") var descriptiveTags: MutableList<TagItem>,
    @SerializedName("tags") var equipTagsList: MutableList<TagItem>,
    @SerializedName("bacnetProperties") val bacnetProperties: MutableList<BacnetProperty>,
    // used to show and hide using arrow
    @SerializedName("displayInEditor")  var displayInEditor: MutableState<Boolean> = mutableStateOf(false),
    // used to show and hide point in zone view
    @SerializedName("displayInUi")  var displayInUi: MutableState<Boolean> = mutableStateOf(false),
    @SerializedName("disName") val disName: String = "",
    @SerializedName("defaultWriteLevel") var defaultWriteLevel: String = "8",
    var isSchedulable: MutableState<Boolean> = mutableStateOf(false),
)

