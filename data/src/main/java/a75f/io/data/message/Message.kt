package a75f.io.data.message

import androidx.room.*
import com.google.gson.annotations.SerializedName

@Entity(tableName = "messages")
data class Message(@PrimaryKey val messageId: String,
    @ColumnInfo(name = MESSAGE_ATTRIBUTE_COMMAND)
    var command: String? = null,
    @ColumnInfo(name = MESSAGE_ATTRIBUTE_ID)
    var id: String? = null,
    @ColumnInfo(name = MESSAGE_ATTRIBUTE_IDS)
    var ids: List<String>? = null,
    @ColumnInfo(name = MESSAGE_ATTRIBUTE_VAL)
    @SerializedName("val")
    var value: String? = null,
    @ColumnInfo(name = MESSAGE_ATTRIBUTE_WHO)
    var who: String? = null,
    @ColumnInfo(name = MESSAGE_ATTRIBUTE_LEVEL)
    var level: Int? = null,
    @ColumnInfo(name = MESSAGE_ATTRIBUTE_REMOTE_CMD_TYPE)
    var remoteCmdType: String? = null,
    @ColumnInfo(name = MESSAGE_ATTRIBUTE_REMOTE_CMD_LEVEL)
    var remoteCmdLevel: String? = null,
    @ColumnInfo(name = MESSAGE_ATTRIBUTE_VERSION)
    var version: String? = null,
    @ColumnInfo(name = MESSAGE_ATTRIBUTE_TIME_TOKEN)
    var timeToken: Long = 0,
    @ColumnInfo(name = MESSAGE_ATTRIBUTE_RECEIVED_TIME)
    var receivedTime: Long = 0,
    @ColumnInfo(name = MESSAGE_ATTRIBUTE_HANDLED_TIME)
    var handledTime: Long = 0,
    @ColumnInfo(name = MESSAGE_ATTRIBUTE_HANDLING_STATUS)
    var handlingStatus: Boolean = false,
    @ColumnInfo(name = MESSAGE_ATTRIBUTE_RETRY_COUNT)
    var retryCount: Int = 0,
    @ColumnInfo(name = MESSAGE_ATTRIBUTE_ERR_MESSAGE)
    var error: String? = "",
    @ColumnInfo(name = MESSAGE_ATTRIBUTE_AUTO_CX_STOP_TIME)
    var autoCXStopTime: String? = "",
    @ColumnInfo(name = MESSAGE_ATTRIBUTE_AUTO_CX_STATE)
    var autoCXState: Int = 0,
    @ColumnInfo(name = MESSAGE_ATTRIBUTE_LOG_LEVEL)
                   var loglevel: String? = ""
)
