package a75f.io.sitesequencer

import a75f.io.api.haystack.Alert
import com.google.gson.annotations.SerializedName

data class SiteSequencerDefinition(
    @SerializedName("seqId")
    val seqId: String,
    @SerializedName("workspaceId")
    val workspaceId: String,
    @SerializedName("siteRef")
    val siteRef: String,
    @SerializedName("seqName")
    val seqName: String,
    @SerializedName("category")
    val category: String?,
    @SerializedName("blocklyXml")
    val blocklyXml: String,
    @SerializedName("snippet")
    val snippet: String,
    @SerializedName("modifiedBy")
    val modifiedBy: SequenceModifiedBy,
    @SerializedName("enabled")
    val enabled: Boolean,
    @SerializedName("logsEnabled")
    val logsEnabled: Boolean,
    @SerializedName("parsingSuccess")
    val parsingSuccess: Boolean,
    @SerializedName("functionIds")
    val functionIds: String?,
    @SerializedName("scope")
    val scope: ArrayList<String>?,
    @SerializedName("seqAlerts")
    val seqAlerts: ArrayList<SequenceAlert>?,
    @SerializedName("quartzCronRequest")
    val quartzCronRequest: QuartzCronRequest
)

data class SequenceAlert(
    @SerializedName("alertBlockId")
    val alertBlockId: String,
    @SerializedName("alertType")
    val alertType: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("notificationMessage")
    val notificationMessage: String,
    @SerializedName("severity")
    val severity: String,
    @SerializedName("enabled")
    val enabled: Boolean,
    @SerializedName("alertScope")
    val alertScope: Any?,
    @SerializedName("alertDefinitionId")
    val alertDefinitionId: String,
)

data class SequenceModifiedBy(
    @SerializedName("userEmailId")
    val userEmailId: String,
    @SerializedName("userId")
    val userId: String,
    @SerializedName("dateTime")
    val dateTime: String
)

data class QuartzCronRequest(
    @SerializedName("frequency")
    val frequency: String,
    @SerializedName("hour")
    val hour: Int?,
    @SerializedName("minute")
    val minute: Int?,
    @SerializedName("second")
    val second: Int?,
    @SerializedName("dayOfWeek")
    val dayOfWeek: String?,
)