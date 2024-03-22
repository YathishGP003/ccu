package a75f.io.restserver.server

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HisItem
import a75f.io.api.haystack.util.LevelData
import a75f.io.api.haystack.util.ReadAllResponse
import a75f.io.api.haystack.util.retrieveLevelValues
import a75f.io.device.bacnet.BacnetConfigConstants
import a75f.io.device.bacnet.BacnetConfigConstants.HTTP_SERVER_STATUS
import a75f.io.device.bacnet.BacnetConfigConstants.ZONE_TO_VIRTUAL_DEVICE_MAPPING
import a75f.io.device.bacnet.readExternalBacnetJsonFile
import a75f.io.device.bacnet.updateBacnetHeartBeat
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ContentNegotiation
import io.ktor.features.gzip
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.WebSockets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import org.projecthaystack.HDict
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HGrid
import org.projecthaystack.HGridBuilder
import org.projecthaystack.HRow
import org.projecthaystack.HVal
import org.projecthaystack.UnknownRecException
import org.projecthaystack.io.HZincReader
import org.projecthaystack.io.HZincWriter
import java.util.concurrent.TimeUnit

class HttpServer {

    private val PORT = 5001
    private val HTTP_SERVER = "HttpServer"

    companion object{
        var sharedPreferences: SharedPreferences? = null
        private var instance: HttpServer? = null
        fun getInstance(context: Context): HttpServer? {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            if (instance == null) {
                instance = HttpServer()
            }
            return instance
        }
    }

    fun isVirtualZoneEnabled() : Boolean{
        var isVirtualZoneEnabled = false
        val confString: String? = sharedPreferences!!.getString(BacnetConfigConstants.BACNET_CONFIGURATION, null)
        if (confString != null) {
            try {
                isVirtualZoneEnabled = JSONObject(confString).getBoolean(ZONE_TO_VIRTUAL_DEVICE_MAPPING)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        return isVirtualZoneEnabled
    }

    fun startServer() {
        CoroutineScope(Dispatchers.IO).launch {
            server.start(wait = true)
        }
        sharedPreferences!!.edit().putBoolean(HTTP_SERVER_STATUS, true).apply()
        Log.d(L.TAG_CCU_BACNET, "server started.")
    }

    fun stopServer() {
        server.stop(1_000, 2_000)
        instance = null
        sharedPreferences!!.edit().putBoolean(HTTP_SERVER_STATUS, false).apply()
        Log.d(L.TAG_CCU_BACNET, "server stopped.")
    }

    val server by lazy {
        embeddedServer(Netty, PORT, watchPaths = emptyList()) {
            install(WebSockets)
            install(CallLogging)
            // provides the automatic content conversion of requests based on theirContent-Type
            // and Accept headers. Together with the json() setting, this enables automatic
            // serialization and deserialization to and from JSON – allowing
            // us to delegate this tedious task to the framework.
            install(ContentNegotiation) {
                gson {
                    setPrettyPrinting()
                    disableHtmlEscaping()
                }
            }
            // configures Cross-Origin Resource Sharing. CORS is needed to make calls from arbitrary
            // JavaScript clients, and helps us prevent issues down the line.
            install(CORS) {
                method(HttpMethod.Get)
                method(HttpMethod.Post)
                method(HttpMethod.Delete)
                anyHost()
            }
            // Greatly reduces the amount of data that's needed to be sent to the client by
            // gzipping outgoing content when applicable.
            install(Compression) {
                gzip()
            }
            routing {

                get("/read/{query}") {
                    CcuLog.i(HTTP_SERVER," read: "+call.parameters["query"])
                    val query = call.parameters["query"]
                    if (query != null) {
                        call.respond(HttpStatusCode.OK, BaseResponse(HZincWriter.gridToString(
                            HGridBuilder.dictToGrid(CCUHsApi.getInstance()
                                .getHSClient().read(query)))))
                    }  else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }

                get("/hisRead/{query}") {
                    CcuLog.i(HTTP_SERVER," hisRead: "+call.parameters["query"])
                    val query = call.parameters["query"]
                    if (query != null) {
                        val isHeartBeatPoint = isHeartBeatPoint("heartbeat", query)
                        if (isHeartBeatPoint) {
                            CcuLog.i(HTTP_SERVER, "this is hearbeat point")
                            val hisItem = getHisItemByIdAndRange(query, "current")
                            if (hisItem != null) {
                                CcuLog.i(HTTP_SERVER, "his item is present")
                                val lastModifiedTimeInMillis = hisItem.dateInMillis
                                val currentTimeInMillis = System.currentTimeMillis()
                                val diffTime =
                                    TimeUnit.MILLISECONDS.toMinutes(currentTimeInMillis - lastModifiedTimeInMillis)
                                CcuLog.i(
                                    HTTP_SERVER,
                                    " currentTimeInMillis:==> $currentTimeInMillis <--lastmodified-->$lastModifiedTimeInMillis --diff-- $diffTime"
                                )
                                if (diffTime > 15) {
                                    call.respond(
                                        HttpStatusCode.OK, BaseResponse(
                                            0
                                        )
                                    )
                                } else {
                                    call.respond(
                                        HttpStatusCode.OK, BaseResponse(
                                            hisItem.`val`
                                        )
                                    )
                                }
                            } else {
                                CcuLog.i(HTTP_SERVER, "his item not present")
                                call.respond(
                                    HttpStatusCode.OK, BaseResponse(
                                        CCUHsApi.getInstance()
                                            .readHisValById(query)
                                    )
                                )
                            }
                        } else {
                            call.respond(HttpStatusCode.OK, BaseResponse(CCUHsApi.getInstance()
                                .readHisValById(query)))
                        }
                    }else{
                        call.respond(HttpStatusCode.NotFound)
                    }
                }

                get("/readAll/{query}") {
                    val query = call.parameters["query"]
                    val isVirtualZoneEnabled = isVirtualZoneEnabled()
                    CcuLog.i(HTTP_SERVER, "read all query: $query <-isVirtualZoneEnabled-> $isVirtualZoneEnabled")
                    var equipRefId = ""
                    if (query != null) {
                        var group = ""
                        if(query.contains("equipRef")){
                            try {
                                equipRefId = getEquipRefId(query).replace("@","").trim()
                                group = CCUHsApi.getInstance().readMapById(equipRefId)["group"] as String
                            }catch (e: UnknownRecException){
                                e.printStackTrace()
                            }
                        }
                        val tempGrid = CCUHsApi.getInstance().getHSClient().readAll(query)
                        val mutableDictList = repackagePoints(tempGrid, isVirtualZoneEnabled, group)
                        val finalGrid = HGridBuilder.dictsToGrid(mutableDictList.toTypedArray())
                        val modifiedGridResponse = HZincWriter.gridToString(finalGrid)
                        if(query.contains("point")){
                            val levelData =  getLevelValues(finalGrid)
                            val fullResponse = ReadAllResponse(modifiedGridResponse, levelData)
                            CcuLog.i(HTTP_SERVER, " fullResponse: ${BaseResponse(fullResponse)}")
                            call.respond(HttpStatusCode.OK, BaseResponse(fullResponse))
                        }else {
                            CcuLog.i(HTTP_SERVER, " response: ${BaseResponse(modifiedGridResponse)}")
                            call.respond(HttpStatusCode.OK, BaseResponse(modifiedGridResponse))
                        }
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }

                get("/bacnet/config") {
                    CcuLog.i(HTTP_SERVER,"called API: /bacnet/config ")
                    val response = readExternalBacnetJsonFile()
                    CcuLog.i(HTTP_SERVER, " response: $response")
                    call.respond(HttpStatusCode.OK, response)
                }

                get("/bacnet/heartbeat") {
                    CcuLog.i(HTTP_SERVER,"called API: /bacnet/heartbeat ")
                    updateBacnetHeartBeat();
                }

                post("/watchSub") {
                    CcuLog.i(HTTP_SERVER," watch sub: ")
                    val body = call.receive<String>()
                    if (body != null) {
                        val hGrid = retrieveGridFromRequest(body)
                        val watchSubRequest = CCUHsApi.getInstance().hsClient.watchSubscribe(
                            hGrid
                        )
                        CcuLog.i(HTTP_SERVER, "check values in response ${watchSubRequest.isEmpty}")
                        call.respondText(HZincWriter.gridToString(watchSubRequest), ContentType.Any , HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }

                post("/watchUnSub") {
                    CcuLog.i(HTTP_SERVER," watch un sub: ")
                    val body = call.receive<String>()
                    if (body != null) {
                        val hGrid = retrieveGridFromRequest(body)
                        val watchSubRequest = CCUHsApi.getInstance().hsClient.watchUnSubscribe(
                            hGrid
                        )
                        CcuLog.i(HTTP_SERVER, "check values in response ${watchSubRequest.isEmpty}")
                        call.respondText(HZincWriter.gridToString(watchSubRequest), ContentType.Any , HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }

                post("/watchPoll") {
                    val body = call.receive<String>()
                    if (body != null) {
                        val hGrid = retrieveGridFromRequest(body)
                        val watchPollResponse = CCUHsApi.getInstance().hsClient.watchPoll(
                            hGrid
                        )
                        CcuLog.i(HTTP_SERVER, "check values in response ${watchPollResponse.isEmpty}")
                        if(!watchPollResponse.isEmpty){
                            CcuLog.i(HTTP_SERVER, "no of rows in response ${watchPollResponse.numRows()}")
                        }
                        if(!watchPollResponse.isErr){
                            call.respondText(HZincWriter.gridToString(watchPollResponse), ContentType.Any , HttpStatusCode.OK)
                        }else{
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }

                //example call = http://127.0.0.1:5001/pointWrite/6a1f6539-86dd-48d3-be6c-0ae0b50fa388
                get("/pointWrite/{id}") {
                    CcuLog.i(HTTP_SERVER, "called API: /pointWrite/{id} ")
                    val id = call.parameters["id"]
                    val response = CCUHsApi.getInstance().readPointArr("@"+id);
                    CcuLog.i(HTTP_SERVER, " response: $response")
                    call.respond(HttpStatusCode.OK, BaseResponse(response))
                }

                //example call = http://127.0.0.1:5001/pointWrite?id=6a1f6539-86dd-48d3-be6c-0ae0b50fa388&level=1&val=7.5&who=bacnet&duration=200000
                get("/pointWrite") {
                    CcuLog.i(HTTP_SERVER, "called API: /pointWrite")
                    val id = call.parameters["id"]
                    val level = call.parameters["level"]
                    var value = call.parameters["val"]
                    val who = call.parameters["who"]
                    var duration : String? = call.parameters["duration"]

                    if(id == null || level == null || who == null || duration == null) {
                        call.respond(HttpStatusCode.NotFound, BaseResponse( "Invalid request"))
                    }else{
                        // if level is coming null ie. user wanted to reset level
                        if(value == "null"){
                            value = "0"
                            duration = "1000"
                        }
                        val pointGrid = CCUHsApi.getInstance().writePoint(id, level.toInt(), who, value!!.toDouble(), duration!!.toInt())
                        if (pointGrid != null) {
                            if(!pointGrid.isEmpty || !pointGrid.isErr)
                                call.respond(HttpStatusCode.OK, BaseResponse(HttpStatusCode.OK));
                            else
                                call.respond(HttpStatusCode.OK, BaseResponse(HttpStatusCode.NoContent));
                        }else{
                            call.respond(HttpStatusCode.OK, BaseResponse(HttpStatusCode.NoContent))
                        }
                    }
                }
            }
        }
    }

    private fun getEquipRefId(input: String): String {
            val regex = "@[a-fA-F0-9\\-]+".toRegex()
            val matches = regex.findAll(input)
            for (match in matches) {
                return match.value
            }
        return ""
    }

    private fun getLevelValues(tempGrid: HGrid): MutableList<LevelData> {
        val mutableList = mutableListOf<LevelData>()
        for (row in tempGrid) {
            val id = (row as HRow).get("id")
            mutableList.add(LevelData(id.toString(), retrieveLevelValues(id.toString())))
        }
        return mutableList
    }

    private fun retrieveGridFromRequest(response: String): HGrid? {
        val zReader = HZincReader(response)
        return zReader.readGrid()
    }

    private fun isHeartBeatPoint(filterKey : String, pointId : String): Boolean {
        val pointMap = CCUHsApi.getInstance().readMapById(pointId)
        if(pointMap != null && pointMap[filterKey] != null){
            return true
        }
        return false
    }

    private fun getHisItemByIdAndRange(pointId: String, range: String): HisItem? {
        var hisItem: HisItem? = null
        val list = CCUHsApi.getInstance().hisRead(pointId, range)
        if (list != null && list.size >= 1) {
            hisItem = list[0]
        }
        return hisItem
    }

    private fun repackagePoints(tempGrid: HGrid, isVirtualZoneEnabled: Boolean, group: String): MutableList<HDict> {
        val mutableDictList = mutableListOf<HDict>()
        val gridIterator = tempGrid.iterator()
        while (gridIterator.hasNext()) {
            val row = gridIterator.next() as HRow
            val rowIterator = row.iterator()
            var hDictBuilder = HDictBuilder()
            var extractedDis = ""
            var extractedGroup = ""
            var extractedZoneRef = ""
            var bacnetId = ""
            var isEquip = false
            var extractedEquipRef = ""
            var isSystem = false
            while (rowIterator.hasNext()) {
                val e: HDict.MapEntry = (rowIterator.next() as HDict.MapEntry)
                when (e.value!!) {
                    is String -> hDictBuilder.add(e.key.toString(), e.value as String)
                    is Long -> hDictBuilder.add(e.key.toString(), e.value as Long)
                    is Double -> hDictBuilder.add(e.key.toString(), e.value as Double)
                    is Boolean -> hDictBuilder.add(e.key.toString(), e.value as Boolean)
                    is HVal -> hDictBuilder.add(e.key.toString(), e.value as HVal)
                    else -> hDictBuilder.add(e.key.toString(), e.value.toString())
                }
                if (e.key.toString() == "system") {
                    isSystem = true
                }
                if (e.key.toString() == "equip") {
                    isEquip = true
                }
                if (e.key.toString() == "dis") {
                    extractedDis = e.value.toString()
                }
                if (e.key.toString() == "group") {
                    extractedGroup = e.value.toString()
                }
                if (e.key.toString() == "roomRef") {
                    extractedZoneRef = e.value.toString()
                }
                if (e.key.toString() == "bacnetId") {
                    bacnetId = e.value.toString()
                }
                if (e.key.toString() == "equipRef") {
                    extractedEquipRef = e.value.toString()
                }
            }
            val zoneName = CCUHsApi.getInstance()
                .readMapById(extractedZoneRef.replace("@", ""))["dis"].toString().trim()
            val pointDisName = extractedDis.split("-")
            val lastLiteralFromDis = pointDisName[pointDisName.size - 1]
            var profileName = ""
            if (pointDisName.size > 1) {
                profileName = pointDisName[1]
            }

            if (!isSystem) {
                if (isVirtualZoneEnabled) {
                    if (isEquip) {
                        hDictBuilder.add("dis", "${zoneName}_${extractedGroup}")
                    } else {
                        hDictBuilder.add("dis", lastLiteralFromDis)
                    }

                    if (group.isNotEmpty()) {
                        bacnetId = bacnetId.replace(group, "").trim()
                    } else if (extractedGroup.isNotEmpty()) {
                        bacnetId = bacnetId.replace(extractedGroup, "").trim()
                    } else if (extractedEquipRef.isNotEmpty()) {
                        extractedEquipRef = extractedEquipRef.replace("@", "").trim()
                        val groupFromEquipRef =
                            CCUHsApi.getInstance().readMapById(extractedEquipRef)["group"] as String
                        bacnetId = bacnetId.replace(groupFromEquipRef, "").trim()
                    }
                    try {
                        if (bacnetId != "0.0") {
                            hDictBuilder.add("bacnetId", bacnetId.toLong())
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    if (zoneName.isEmpty() || zoneName == "null" || zoneName == "") {
                        hDictBuilder.add("dis", "${profileName}_$lastLiteralFromDis")
                    } else {
                        hDictBuilder.add(
                            "dis",
                            "${zoneName}_${profileName}_${extractedGroup}_$lastLiteralFromDis"
                        )
                    }
                }
            }else{
                hDictBuilder.add("dis", lastLiteralFromDis)
            }
            mutableDictList.add(hDictBuilder.toDict())
        }
        return mutableDictList
    }
}
