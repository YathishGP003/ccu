package a75f.io.data.entities

import androidx.room.*

@Entity(tableName = "entities")
data class HayStackEntity(
    @PrimaryKey val Id: String,
    @ColumnInfo(name = "entity_entry") var allEntries:HashMap<String, Any>? = null)