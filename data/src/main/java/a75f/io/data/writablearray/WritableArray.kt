package a75f.io.data.writablearray

import a75f.io.data.Converters
import a75f.io.data.WriteArray
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "writableArray")
data class WritableArray (@PrimaryKey val Id: String,
                          @ColumnInfo(name = "writablePoint") var writablePoint:  String? = null


)

