package a75f.io.data.message

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val messageId: String,
    @ColumnInfo(name = "command") val command: String?,
    @ColumnInfo(name = "id") val id: String?,
    @ColumnInfo(name = "value") val value: String?,
    @ColumnInfo(name = "who") val who: String?,
    @ColumnInfo(name = "level") val level: Int?,
    @ColumnInfo(name = "handlingStatus") var handlingStatus: Boolean?,
    @ColumnInfo(name = "retryCount") var retryCount : Int,
    @ColumnInfo(name = "error") var error: String?
)

