package a75f.io.data.entities

import a75f.io.data.RenatusDatabase
import javax.inject.Singleton


@Singleton
class EntityDatabaseHelper constructor (private val renatusDatabase: RenatusDatabase) : DatabaseHelper {
    override suspend fun insert(entity: HayStackEntity) {
        renatusDatabase.entityDao().insert(entity)
    }

    override suspend fun update(entity: HayStackEntity) {
        renatusDatabase.entityDao().update(entity)
    }

    override suspend fun delete(entity: HayStackEntity) {
        renatusDatabase.entityDao().delete(entity)
    }

    override fun getAllEntities(): List<HayStackEntity> {
       return renatusDatabase.entityDao().getAllEntities()
    }

    override suspend fun insertAll(entity: List<HayStackEntity>) {
        renatusDatabase.entityDao().insertAll(entity)
    }

    override suspend fun updateAll(entity: List<HayStackEntity>) {
        renatusDatabase.entityDao().updateAll(entity)
    }

    override suspend fun deleteAll(entity: List<HayStackEntity>) {
        renatusDatabase.entityDao().deleteAll(entity)
    }

    override suspend fun deletewithId(id: String) {
        renatusDatabase.entityDao().deleteWithId(id)
    }

}