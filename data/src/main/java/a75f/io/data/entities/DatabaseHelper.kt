package a75f.io.data.entities

interface DatabaseHelper {
    suspend fun insert(entity: HayStackEntity)
    suspend fun update(entity: HayStackEntity)
    suspend fun delete(entity: HayStackEntity)
    fun getAllEntities(): List<HayStackEntity>
    suspend fun insertAll(entity: List<HayStackEntity>)
    suspend fun updateAll(entity: List<HayStackEntity>)
    suspend fun deleteAll(entity: List<HayStackEntity>)

    suspend fun deletewithId(id : String)
}