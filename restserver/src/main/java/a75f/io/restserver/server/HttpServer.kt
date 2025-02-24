package a75f.io.restserver.server

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HisItem
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.util.LevelData
import a75f.io.api.haystack.util.ReadAllResponse
import a75f.io.api.haystack.util.retrieveLevelValues
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.interfaces.ModbusDataInterface
import a75f.io.logic.util.bacnet.BacnetConfigConstants.HTTP_SERVER_STATUS
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IS_BACNET_INITIALIZED
import a75f.io.logic.util.bacnet.readExternalBacnetJsonFile
import a75f.io.logic.util.bacnet.updateBacnetHeartBeat
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
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
import org.json.JSONArray
import org.json.JSONObject
import org.projecthaystack.HGrid
import org.projecthaystack.HGridBuilder
import org.projecthaystack.HRef
import org.projecthaystack.HRow
import org.projecthaystack.UnknownRecException
import org.projecthaystack.io.HZincReader
import org.projecthaystack.io.HZincWriter
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class HttpServer {

    private val PORT = 5001
    private val HTTP_SERVER = "HttpServer"

    companion object{
        var modbusDataInterface: ModbusDataInterface? = null
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

    fun isServerRunning(): Boolean {
        return sharedPreferences!!.getBoolean(HTTP_SERVER_STATUS, false)
    }

    fun startServer() {
        CoroutineScope(Dispatchers.IO).launch {
            server.start(wait = true)
        }
        sharedPreferences!!.edit().putBoolean(HTTP_SERVER_STATUS, true).apply()
        CcuLog.d(L.TAG_CCU_BACNET, "server started.")
    }

    fun stopServer() {
        server.stop(1_000, 2_000)
        instance = null
        sharedPreferences!!.edit().putBoolean(HTTP_SERVER_STATUS, false).apply()
        CcuLog.d(L.TAG_CCU_BACNET, "server stopped.")
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
                allowNonSimpleContentTypes = true
                host("*")
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
                            val hisItem = getHisItemByIdAndRange(query, "current")
                            if (hisItem != null) {
                                val lastModifiedTimeInMillis = hisItem.dateInMillis
                                val currentTimeInMillis = System.currentTimeMillis()
                                val diffTime =
                                    TimeUnit.MILLISECONDS.toMinutes(currentTimeInMillis - lastModifiedTimeInMillis)
                                if (diffTime > 15 || hisItem.`val`== null) {
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
                                call.respond(
                                    HttpStatusCode.OK, BaseResponse(
                                        0
                                    )
                                )
                            }
                        } else {
                            val hisItem = CCUHsApi.getInstance().readHisValById(query)
                            CcuLog.i(HTTP_SERVER, "his item present -> $hisItem")
                            call.respond(HttpStatusCode.OK, BaseResponse(hisItem))
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
                                CcuLog.d(HTTP_SERVER, "read all query contains equipRef equipRefId is==> $equipRefId")
                                if(CCUHsApi.getInstance().readMapById(equipRefId)["group"] != null){
                                    group = CCUHsApi.getInstance().readMapById(equipRefId)["group"] as String
                                }
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
                    if (sharedPreferences?.getBoolean(IS_BACNET_INITIALIZED, false) == true) {
                        CcuLog.i(HTTP_SERVER, "called API: /bacnet/config ")
                        val response = readExternalBacnetJsonFile()
                        CcuLog.i(HTTP_SERVER, " response: $response")
                        call.respond(HttpStatusCode.OK, response)
                    }
                }

                get("/bacnet/heartbeat") {
                    CcuLog.i(HTTP_SERVER,"called API: /bacnet/heartbeat ")
                    updateBacnetHeartBeat()
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
                    CcuLog.d(HTTP_SERVER," watchPoll: ")
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
                    val response = CCUHsApi.getInstance().readPointArr("@$id")
                    CcuLog.i(HTTP_SERVER, " response: $response")
                    call.respond(HttpStatusCode.OK, BaseResponse(response))
                }

                //example call = http://127.0.0.1:5001/pointWrite?id=6a1f6539-86dd-48d3-be6c-0ae0b50fa388&level=1&val=7.5&who=bacnet&duration=200000
                get("/pointWrite") {
                    CcuLog.i(HTTP_SERVER, "called API: /pointWrite")
                    val id = call.parameters["id"]
                    var level = call.parameters["level"]
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
                        if(isBacnetClientPoint(id)){
                            level = "8"
                        }
                        CCUHsApi.getInstance()
                            .writeHisValById(id, value!!.toDouble())
                        val pointGrid = CCUHsApi.getInstance().writePoint(id, level!!.toInt(), who, value!!.toDouble(), duration!!.toInt())
                        if (pointGrid != null) {
                            if(!pointGrid.isEmpty || !pointGrid.isErr){
                                call.respond(HttpStatusCode.OK, BaseResponse(HttpStatusCode.OK))
                                updateHeartBeatPoint(id)
                            }
                            else
                                call.respond(HttpStatusCode.OK, BaseResponse(HttpStatusCode.NoContent))
                        }else{
                            call.respond(HttpStatusCode.OK, BaseResponse(HttpStatusCode.NoContent))
                        }
                    }
                }


                // Dashboard API's with JSON format response

                /*
                URL     : 192.168.1.5:5001/hisReadMany
	            Body    :
		            cd0dba2c-032a-41d4-a1db-0e1fc246720d,cd0dba2c-032a-41d4-a1db-0e1fc246720d,cd0dba2c-032a-41d4-a1db-0e1fc246720d
                Response :
                    [{"id":"cd0dba2c-032a-41d4-a1db-0e1fc246720d","value":0,"unit":"kVAR"},
                     {"id":"cd0dba2c-032a-41d4-a1db-0e1fc246720d","value":0,"unit":"kVAR"},
                     {"id":"cd0dba2c-032a-41d4-a1db-0e1fc246720d","value":0,"unit":"kVAR"}]
                 */

                post("/hisReadMany") {
                    val ids = call.receive<String>()
                    CcuLog.i(HTTP_SERVER," hisReadMany: $ids")
                    val responseData = JSONArray()
                    val haystack = CCUHsApi.getInstance()
                    if (ids.isNotBlank()) {
                        val items: Iterator<*> = getHGridData(ids).iterator()
                        while (items.hasNext()) {
                            val entity = items.next() as HRow
                            val id = entity["id"].toString()
                            val item = JSONObject()
                            item.put("id", entity["id"])
                            item.put("value", haystack.readHisValById(id))
                            if (entity.has("unit")) {
                                item.put("unit", entity["unit"])
                            }
                            responseData.put(item)
                            CcuLog.i(HTTP_SERVER, "item : $item")
                        }
                        call.respond(HttpStatusCode.OK, responseData.toString())
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Invalid request")
                    }
                }

                /*
               URL     : 192.168.1.5:5001/pointWriteMany
               Body    :
                   cd0dba2c-032a-41d4-a1db-0e1fc246720d,cd0dba2c-032a-41d4-a1db-0e1fc246720d,cd0dba2c-032a-41d4-a1db-0e1fc246720d
               Response :
                   [{"id":"cd0dba2c-032a-41d4-a1db-0e1fc246720d","value":0,"unit":"kVAR"},
                    {"id":"cd0dba2c-032a-41d4-a1db-0e1fc246720d","value":0,"unit":"kVAR"},
                    {"id":"cd0dba2c-032a-41d4-a1db-0e1fc246720d","value":0,"unit":"kVAR"}]
                */

                post("/pointWriteMany") {
                    val ids = call.receive<String>()
                    CcuLog.i(HTTP_SERVER," pointWriteMany: $ids")
                    val responseData = JSONArray()
                    if (ids.isNotBlank()) {
                        val items: Iterator<*> = getHGridData(ids).iterator()
                        while (items.hasNext()) {
                            val entity = items.next() as HRow
                            if (entity.has("writable")) {
                                val id = entity["id"].toString()
                                val item = JSONObject()
                                item.put("id", entity["id"])
                                item.put("value", readWritablePointValue(id))
                                if (entity.has("unit")) {
                                    item.put("unit", entity["unit"])
                                }
                                responseData.put(item)
                                CcuLog.i(HTTP_SERVER, "item : $item")
                            }
                        }
                        call.respond(HttpStatusCode.OK, responseData.toString())
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Invalid request")
                    }
                }

                /*
               URL     : 192.168.1.5:5001/query
               Body    :
                   tuner and point
               Response :
                     [
                     {"dab":"marker","tz":"Kolkata","roomRef":"SYSTEM","createdDateTime":"2025-01-03T12:08:06.232+00:00 UTC","point":"marker"},
                     {"dab":"marker","tz":"Kolkata","roomRef":"SYSTEM","createdDateTime":"2025-01-03T12:08:06.232+00:00 UTC","point":"marker"},
                     {"dab":"marker","tz":"Kolkata","roomRef":"SYSTEM","createdDateTime":"2025-01-03T12:08:06.232+00:00 UTC","point":"marker"}]
                */

                post("/query") {
                    val query = call.receive<String>()
                    CcuLog.i(HTTP_SERVER, " /query  : $query")

                    if (query != null) {
                        val entities = CCUHsApi.getInstance().readAllEntities(getModifiedQuery(query))
                        if (entities.isNullOrEmpty()) {
                            call.respond(HttpStatusCode.BadRequest, "No points found for the query")
                        } else {
                            val response = JSONArray()
                            entities.forEach { entity ->
                                val responseEntity = JSONObject()
                                entity.forEach { (key, value) ->
                                    responseEntity.put(key as String, value)
                                }
                                response.put(responseEntity)
                            }
                            call.respond(HttpStatusCode.OK, response.toString())
                        }
                    } else {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }

                /*
                URL     : 192.168.1.5:5001/writeManyDefaultValues
                Body    :
                   2fe44a56-919f-46b7-8741-1ea0467aeb78 = 2,
                 */

                post("/writeManyDefaultValues") {
                    val body = call.receive<String>()
                    CcuLog.i(HTTP_SERVER, " /writeManyDefaultValues  : $body")
                    if (body.isNotEmpty()) {
                        try {
                            val dataItems = JSONObject(body)
                            val idsStatus = JSONArray()
                            val haystack = CCUHsApi.getInstance()
                            if (dataItems.length() > 0) {
                                dataItems.keys().forEach { key ->
                                    if (key.isEmpty()) {
                                        return@forEach
                                    }
                                    val id = key.trim()
                                    val value = dataItems.get(id)
                                    val entity = haystack.readMapById(id)
                                    if (entity.isEmpty()) {
                                        idsStatus.put(JSONObject().put(id, "Id not found"))
                                    } else if (entity.containsKey(Tags.WRITABLE)) {
                                        when (value) {
                                            is Number -> {
                                                haystack.writeDefaultValById(id, value.toDouble())
                                                haystack.writeHisValById(id, haystack.readPointPriorityVal(id))
                                                idsStatus.put(JSONObject().put(id, "Updated successfully"))
                                            }
                                            is String -> {
                                                haystack.writeDefaultValById(id, value)
                                                idsStatus.put(JSONObject().put(id, "Updated successfully"))
                                            }
                                            else -> {}
                                        }
                                    } else {
                                        idsStatus.put(JSONObject().put(id, "ID does not have writable tag"))
                                    }
                                }
                            } else {
                                call.respond(
                                    HttpStatusCode.NotFound,
                                    BaseResponse("Invalid request")
                                )
                            }
                            call.respond(HttpStatusCode.OK, idsStatus.toString())

                        } catch (e: Exception) {
                            e.printStackTrace()
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                BaseResponse("Exception ${e.message} ${e.stackTrace}")
                            )
                        }
                    } else {
                        call.respond(HttpStatusCode.NotFound, BaseResponse("Invalid request"))
                    }
                }

                get("/getDashboardConfiguration") {
                    CcuLog.i(HTTP_SERVER," getDashboardConfiguration")
                    call.respond(HttpStatusCode.OK, getDashboardConfiguration())
                }
            }
        }
    }

    private fun isBacnetClientPoint(id: String): Boolean {
        var pointId = id
        if(!pointId.startsWith("@")){
            pointId = "@$pointId"
        }
        val pointMap = CCUHsApi.getInstance().readMapById(pointId)
        return pointMap != null && pointMap["bacnetCur"] != null
    }

    private fun updateHeartBeatPoint(id: String){
        var pointId = id
        if(!pointId.startsWith("@")){
            pointId = "@$pointId"
        }
        val point = CCUHsApi.getInstance().readMapById(pointId)
        val isBacnetClientPoint = point["bacnetCur"]
        if(isBacnetClientPoint.toString().isNotEmpty()){
            val equipId = point["equip"]
            val heartBeatPointId = CCUHsApi.getInstance().readEntity("point and heartbeat and equipRef== \" $equipId \"")["id"]
            if(heartBeatPointId.toString().isNotEmpty()){
                CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(heartBeatPointId.toString(), 1.0)
            }
            //modbusDataInterface?.refreshScreen(id)
        }
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
        return pointMap != null && pointMap[filterKey] != null
    }

    private fun getHisItemByIdAndRange(pointId: String, range: String): HisItem? {
        var hisItem: HisItem? = null
        try {
            val list = CCUHsApi.getInstance().hisRead(pointId, range)
            if (list != null && list.size >= 1) {
                hisItem = list[0]
                CcuLog.i(HTTP_SERVER, "his item present getHisItemByIdAndRange -> $hisItem")
            }
        }
        catch (e: Exception){
            CcuLog.e(HTTP_SERVER, "Error in getHisItemByIdAndRange: $e")
        }

        return hisItem
    }

    private fun getDashboardConfiguration(): String {
        val sharedPreferences: SharedPreferences =
            Globals.getInstance().applicationContext.getSharedPreferences(
                "dashboard",
                Context.MODE_PRIVATE
            )
        return sharedPreferences.getString("config", "") ?: ""
    }

    private fun getModifiedQuery(query: String) : String {
        var modifiedQuery = query.replace("\\", "")
        modifiedQuery = fixInvertedCommas(modifiedQuery)
        CcuLog.i(HTTP_SERVER, " /modifiedQuery  : $modifiedQuery")
        return modifiedQuery
    }

    private fun fixInvertedCommas(input: String): String {
        // Define the pattern
        val pattern = Pattern.compile("==\\s*([@\\w-]+)")

        // Match the pattern against the input
        val matcher = pattern.matcher(input)

        // StringBuffer to build the modified string
        val result = StringBuffer()

        // Find and replace the pattern
        while (matcher.find()) {
            // Extract the value after "=="
            val extractedValue = matcher.group(1)

            // Add inverted commas around the extracted value
            val replacement = "==\"$extractedValue\""

            // Replace the matched part with the modified value
            matcher.appendReplacement(result, replacement)
        }

        // Append the remaining part of the input
        matcher.appendTail(result)
        val finalResult = result.toString()
        // Print the modified string
        return finalResult
    }

    private fun readWritablePointValue(id: String): String {
        return try {
            CCUHsApi.getInstance().readPointPriorityVal(id).toString()
        } catch (exception: NumberFormatException){
            CCUHsApi.getInstance().readDefaultStrValById(id).toString()
        }
    }

    private fun getHGridData(ids: String): HGrid {
        val idList = ids.split(",")
        val refined = mutableListOf<HRef>()
        idList.forEach { refined.add(HRef.copy(it)) }
        val finalRes = refined.toTypedArray<HRef>()
        return CCUHsApi.getInstance().readHDictByIds(finalRes)
    }
}
