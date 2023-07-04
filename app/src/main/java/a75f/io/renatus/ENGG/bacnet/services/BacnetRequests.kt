package a75f.io.renatus.ENGG.bacnet.services

import com.google.gson.annotations.SerializedName

data class BacnetReadRequest(
    @SerializedName("destination")
    private val destination: Destination,

    @SerializedName("rp_request")
    private val readRequest: ReadRequest
)

data class BacnetReadRequestMultiple(
    @SerializedName("destination")
    private val destination: Destination,

    @SerializedName("rpm_request")
    private val rpmRequest: RpmRequest
)


data class BacnetWriteRequest(
    @SerializedName("destination")
    private val destination: Destination,

    @SerializedName("wp_request")
    private val writeRequest: WriteRequest
)

data class BacnetWhoIsRequest(
    @SerializedName("who_is_request")
    private val WhoIsRequest: WhoIsRequest? = null,

    @SerializedName("unicast_destination")
    private val destination: Destination? = null,

    @SerializedName("broadcast")
    private val broadCast: BroadCast? = null

)

data class Destination(
    @SerializedName("ip_address")
    private val ipAddress: String,
    private val port: Int
)

data class WhoIsRequest(
    @SerializedName("device_instance_range_low_limit")
    private val deviceInstanceRangeLowLimit: Int,

    @SerializedName("device_instance_range_high_limit")
    private val deviceInstanceRangeHighLimit: Int
)

data class BroadCast(
    @SerializedName("type")
    private val type: String,
)

data class WriteRequest(

    @SerializedName("object_identifier")
    private val objectIdentifier: ObjectIdentifierBacNet,

    @SerializedName("property_value")
    private val propertyValue: PropertyValueBacNet,

    @SerializedName("priority")
    private val priority: Int,

    @SerializedName("property_identifier")
    private val propertyIdentifier: Int,

    @SerializedName("property_array_index")
    private val propertyArrayIndex: Int? = null
)

data class PropertyValueBacNet(
    @SerializedName("type")
    private val type: Int,

    @SerializedName("value")
    private val value: Int,
)

data class RpmRequest(
    @SerializedName("read_access_specifications")
    private val readRequest: MutableList<ReadRequestMultiple>
)

data class ReadRequestMultiple(
    @SerializedName("object_identifier")
    private val objectIdentifier: ObjectIdentifierBacNet,

    @SerializedName("property_references")
    private val propertyReferences: MutableList<PropertyReference>,
)

data class PropertyReference(
    @SerializedName("property_identifier")
    private val propertyIdentifier: Int,

    @SerializedName("property_array_index")
    private val propertyArrayIndex: Int? = null
)

data class ReadRequest(

    @SerializedName("object_identifier")
    private val objectIdentifier: ObjectIdentifierBacNet,

    @SerializedName("property_identifier")
    private val propertyIdentifier: Int,

    @SerializedName("property_array_index")
    private val propertyArrayIndex: Int? = null
)

data class ObjectIdentifierBacNet(
    @SerializedName("object_type")
    private val objectType: Int,

    @SerializedName("object_instance")
    private val objectInstance: Int
)