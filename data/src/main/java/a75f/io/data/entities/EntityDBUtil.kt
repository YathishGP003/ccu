package a75f.io.data.entities

import a75f.io.data.RenatusDatabaseBuilder
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
private val TAG = "Room_DB"

private var entityDbHelper : EntityDatabaseHelper? = null
fun insert(entitytable: HayStackEntity, context: Context) {
    appScope.launch {
        if (entityDbHelper == null) {
            entityDbHelper = EntityDatabaseHelper(RenatusDatabaseBuilder.getInstance(context))
        }
        Log.i(TAG, " DbUtil:Insert entitytable :${entitytable.Id}")
        try {
            Log.i(TAG, " DbUtil:Insert entitytable :-->$entitytable.")
            entityDbHelper?.insert(entitytable)
            Log.i(TAG, " DbUtil:Insert entitytable :<--")
        } catch (e : Exception) {
            e.printStackTrace()
            Log.i(TAG, " Insert Failed $e")
            throw e
        }
    }
}
fun update(entitytable: HayStackEntity, context : Context) {
    appScope.launch {
        if (entityDbHelper == null) {
            entityDbHelper = EntityDatabaseHelper(RenatusDatabaseBuilder.getInstance(context))
        }
        Log.i(TAG, " DbUtil:Update entitytable : ${entitytable.Id}")
        entityDbHelper?.update(entitytable)
    }
}
fun deleteEntityTable(entitytable: HayStackEntity, context : Context) {
    appScope.launch {
        Log.i(TAG, " DbUtil:Delete entitytable : ${entitytable.Id}")
        if (entityDbHelper == null) {
            entityDbHelper = EntityDatabaseHelper(RenatusDatabaseBuilder.getInstance(context))
        }
        entityDbHelper?.delete(entitytable)
    }
}

fun updateEntityTable(entitytable: HayStackEntity) {
    appScope.launch {
        if (entitytable != null) {
            entityDbHelper?.update(entitytable)
        }
    }
}


fun deleteEntitywithId(id: String, context : Context) {
    appScope.launch {
        Log.i(TAG, " DbUtil:Delete entitytable : ${id}")
        if (entityDbHelper == null) {
            entityDbHelper = EntityDatabaseHelper(RenatusDatabaseBuilder.getInstance(context))
        }
        entityDbHelper?.deletewithId(id)
    }
}



