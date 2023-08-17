package a75f.io.logic.migration

import a75f.io.api.haystack.CCUHsApi

interface Migration {
    val hayStack : CCUHsApi
    fun isMigrationRequired() : Boolean
    fun doMigration()
}