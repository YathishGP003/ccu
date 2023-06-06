package a75f.io.domain.config

import a75f.io.domain.api.Entity
import a75f.io.domain.api.EntityConfig

/**
 * EntityConfiguration holds list of points to be added , updated or deleted.
 * This list will be consumed by EntityBuilder to add/update/delete entities.
 */
open class EntityConfiguration {
    val tobeAdded = mutableListOf<EntityConfig>()
    val tobeDeleted = mutableListOf<EntityConfig>()
    val tobeUpdated = mutableListOf<EntityConfig>()
}