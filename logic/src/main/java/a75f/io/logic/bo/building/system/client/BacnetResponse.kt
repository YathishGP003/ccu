package a75f.io.logic.bo.building.system.client

import com.google.gson.annotations.SerializedName


data class ReadResponse(
    val source: Source,
    @SerializedName("rp_response")
    val rpResponse: RpResponse,
    val error: BacnetError?,
    @SerializedName("abort")
    val errorAbort: BacnetErrorAbort?,
    @SerializedName("bacapperror")
    val errorBacApp: BacnetAppError?,
    @SerializedName("reject")
    val errorReject: BacnetAppErrorReject?,
    @SerializedName("a_side_error")
    val errorASide: BacnetAppASideError?
)

data class MultiReadResponse(
    val source: String,//Source,
    @SerializedName("rpm_response")
    val rpResponse: RpResponseMultiRead,
    val error: BacnetError?,
    @SerializedName("abort")
    val errorAbort: BacnetErrorAbort?,
    @SerializedName("bacapperror")
    val errorBacApp: BacnetAppError?,
    @SerializedName("reject")
    val errorReject: BacnetAppErrorReject?,
    @SerializedName("a_side_error")
    val errorASide: BacnetAppASideError?

)

data class Source(
    @SerializedName("ip_address")
    private val ipAddress: String,
    private val port: Int
)

data class RpResponse(
    @SerializedName("object_identifier")
    val objectIdentifier: ObjectIdentifierBacNetResp,

    @SerializedName("property_identifier")
    val propertyIdentifier: String,

    @SerializedName("property_array_index")
    val propertyArrayIndex: String? = null,

    @SerializedName("property_value")
    val propertyValue: PropertyValue

)

data class RpResponseMultiRead(
    @SerializedName("read_access_results")
    val listOfItems : MutableList<RpResponseMultiReadItem>
)

data class RpResponseMultiReadItem(
    @SerializedName("object_identifier")
    val objectIdentifier: ObjectIdentifierBacNetResp,


    @SerializedName("results")
    val results : MutableList<MultiReadResultItem>
)

data class MultiReadResultItem(
    @SerializedName("property_identifier")
    val propertyIdentifier: String,

    @SerializedName("property_array_index")
    val propertyArrayIndex: String? = null,

    @SerializedName("property_value")
    val propertyValue: PropertyValue
)


data class PropertyValue(
    val type: String,
    val value: String,
)

data class ObjectIdentifierBacNetResp(
    @SerializedName("object_type")
    val objectType: String,

    @SerializedName("object_instance")
    val objectInstance: String
)

data class BacnetError(
    @SerializedName("error_code")
    val errorCode: String,

    @SerializedName("error_class")
    val errorClass: String
)

data class BacnetErrorAbort(
    @SerializedName("abort_reason")
    val abortReason: String
)

data class BacnetAppError(
    @SerializedName("error_code")
    val abortReason: String
)

data class BacnetAppErrorReject(
    @SerializedName("reject_reason")
    val abortReason: String
)

data class BacnetAppASideError(
    @SerializedName("reason")
    val abortReason: String
)

data class WriteResponse(
    val error: BacnetError?,
    @SerializedName("abort")
    val errorAbort: BacnetErrorAbort?,
    @SerializedName("bacapperror")
    val errorBacApp: BacnetAppError?,
    @SerializedName("reject")
    val errorReject: BacnetAppErrorReject?,
    @SerializedName("a_side_error")
    val errorASide: BacnetAppASideError?,
    @SerializedName("bacapp_error")
    val bacappError: BacnetError?,
)

data class WhoIsResponse(
    @SerializedName("Iam_response")
    val whoIsResponseList : MutableList<WhoIsResponseItem>,

    val error: BacnetError?
)

data class WhoIsResponseItem(
    @SerializedName("device_identifier")
    val deviceIdentifier : String,
    @SerializedName("vendor_identifier")
    val vendorIdentifier: String,
    @SerializedName("ip_address")
    val ipAddress: String,
    @SerializedName("port_number")
    val portNumber: String,
    @SerializedName("network_number")
    val networkNumber: String,
    @SerializedName("mac_address")
    val macAddress: String?,
)


data class ItemsViewModel(val textType: String, val textValue: String?, val objectType: String) {
}