package a75f.io.logic.interfaces

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import com.google.gson.JsonObject

interface ProfileConfigurationHandlerInterface {
    fun handleProfileConfigPointUpdate(hayStack: CCUHsApi, pointUid: String, msgObject: JsonObject, localPoint: Point): Boolean
}