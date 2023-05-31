package a75f.io.domain.api

import a75f.io.domain.logic.DomainManager

/**
 * An entity definition that binds its domainName to database UUID.
 * domainName - domainName of the entity defined in model
 * id - uuid of the entity instance in haystack
 */
open class Entity (val domainName : String, val id : String)


/**
 * APIs to access entities by the domain name.
 */
class Domain {
    companion object {
        fun getEntity(domainName: String): Entity {
            TODO()
        }

        fun addEntity(entity: Entity) {
            DomainManager.updateDomain(entity)
        }
    }
}