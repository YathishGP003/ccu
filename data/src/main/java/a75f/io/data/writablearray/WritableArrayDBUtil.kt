package a75f.io.data.writablearray

import a75f.io.data.RenatusDatabaseBuilder
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
private val TAG = "Room_DB"

private var writableDbHelper : WritableArrayDatabaseHelper? = null
fun insert(writableArray: WritableArray, context: Context) {
    appScope.launch {
        if (writableDbHelper == null) {
            writableDbHelper = WritableArrayDatabaseHelper(RenatusDatabaseBuilder.getInstance(context))
        }
        Log.i(TAG, " DbUtil:Insert writabelArray :${writableArray.Id}")
        try {
            Log.i(TAG, " DbUtil:Insert writableArray :-->")
            writableDbHelper?.insert(writableArray)
            Log.i(TAG, " DbUtil:Insert writableArray :<--")
        } catch (e : Exception) {
            e.printStackTrace()
            Log.i(TAG, " Insert Failed $e")
        }
    }
}
fun update(writableArray: WritableArray, context : Context) {
    appScope.launch {
        if (writableDbHelper == null) {
            writableDbHelper = WritableArrayDatabaseHelper(RenatusDatabaseBuilder.getInstance(context))
        }
        Log.i(TAG, " DbUtil:Update writableArray : ${writableArray.Id}")
        writableDbHelper?.update(writableArray)
    }
}
fun deleteEntityTable(writableArray: WritableArray, context : Context) {
    appScope.launch {
        Log.i(TAG, " DbUtil:Delete writableArray : ${writableArray.Id}")
        if (writableDbHelper == null) {
            writableDbHelper = WritableArrayDatabaseHelper(RenatusDatabaseBuilder.getInstance(context))
        }
        writableDbHelper?.delete(writableArray)
    }
}

fun updateEntityTable(writableArray: WritableArray) {
    appScope.launch {
        if (writableArray != null) {
            writableDbHelper?.update(writableArray)
        }
    }
}

fun getAllWritableArray(context: Context) : List<WritableArray> {
    var allWritableArray : MutableList<WritableArray> = mutableListOf()
    if (writableDbHelper == null) {
        Log.i(TAG, "writableDbHelper is null ")
        writableDbHelper = WritableArrayDatabaseHelper(RenatusDatabaseBuilder.getInstance(context))
    }
    appScope.launch {
        Log.i(TAG, "getall -> ")
        (writableDbHelper?.getAllwritableArrays())?.toCollection(allWritableArray)
        Log.i(TAG, "getall <- "+allWritableArray.size)
    }
    Log.i(TAG, " allWritableArray size"+allWritableArray.size)
    return allWritableArray
}



fun deleteEntitywithId(id : String , context : Context) {
    appScope.launch {
        Log.i(TAG, " DbUtil:Delete writableArray : ${id}")
        if (writableDbHelper == null) {
            writableDbHelper = WritableArrayDatabaseHelper(RenatusDatabaseBuilder.getInstance(context))
        }
        writableDbHelper?.deleteWithID(id)
    }
}

fun getWritableArrayWithId(context: Context, id : String) : WritableArray? {
    var writabelArray : WritableArray? = null
    if (writableDbHelper == null) {
        Log.i(TAG, "writableDbHelper is null ")
        writableDbHelper = WritableArrayDatabaseHelper(RenatusDatabaseBuilder.getInstance(context))
    }
    appScope.launch {
        Log.i(TAG, "get item -> ")
        writabelArray = writableDbHelper!!.getWritableWithId(id)
        Log.i(TAG, "get item <- "+ writabelArray)
    }

    return writabelArray
}