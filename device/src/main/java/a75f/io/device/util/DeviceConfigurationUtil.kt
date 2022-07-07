package a75f.io.device.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.tuners.TunerUtil
import android.util.Log

/**
 * Created by Manjunath K on 18-04-2022.
 */
class DeviceConfigurationUtil {

    companion object{
        fun getUserConfiguration(): Double{
            Log.i("DEV_DEBUG", "getUserConfiguration: ")
            val useCelsius = CCUHsApi.getInstance().readEntity("displayUnit")
            Log.i("DEV_DEBUG", "getUserConfiguration: "+TunerUtil.getTuner(useCelsius["id"].toString()))
            return TunerUtil.getTuner(useCelsius["id"].toString())
        }
    }
}