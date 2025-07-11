package a75f.io.util

/**
 * Created by Sathishkumar S on 29-05-2025.
 */
interface BacnetRequestUtil {
    fun callbackForBacnetMstpRequest(id:String, level:String, value:String)

    fun callbackForBacnetRequest(id:String, level:String, value:String)
}