package a75f.io.restserver.server

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.bacnet.BacnetConfigConstants.HTTP_SERVER_STATUS
import a75f.io.device.bacnet.readExternalBacnetJsonFile
import a75f.io.device.bacnet.updateBacnetHeartBeat
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.request.receive
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.projecthaystack.HGrid
import org.projecthaystack.HGridBuilder
import org.projecthaystack.io.HZincReader
import org.projecthaystack.io.HZincWriter

class HttpServer {

    private val PORT = 5001

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
                get("/readAll/{query}") {
                    CcuLog.i("HttpServer"," readAll: "+call.parameters["query"])
                    val query = call.parameters["query"]
                    if (query != null) {
                        call.respond(HttpStatusCode.OK, BaseResponse(HZincWriter.gridToString(CCUHsApi.getInstance()
                            .getHSClient().readAll(query))))
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
                get("/read/{query}") {
                    CcuLog.i("HttpServer"," read: "+call.parameters["query"])
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
                    CcuLog.i("HttpServer"," hisRead: "+call.parameters["query"])
                    val query = call.parameters["query"]
                    if (query != null) {
                        call.respond(HttpStatusCode.OK, BaseResponse(CCUHsApi.getInstance()
                                .readHisValById(query)))
                    }  else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }

                get("/readAll/{query}") {
                    CcuLog.i("HttpServer"," readAll: "+call.parameters["query"])
                    val query = call.parameters["query"]
                    CcuLog.i("HttpServer"," qyerry: "+query)


                    if (query != null) {
                        val response = HZincWriter.gridToString(CCUHsApi.getInstance().getHSClient().readAll(query));
                        CcuLog.i("HttpServer", " response: $response")
                        call.respond(HttpStatusCode.OK, BaseResponse(response))
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }

                get("/bacnet/config") {
                    CcuLog.i("HttpServer"," called end point: /bacnet/config ")
                    val query = readExternalBacnetJsonFile()
                    CcuLog.i("HttpServer", " response: $query")
                    call.respond(HttpStatusCode.OK, query)
                }

                get("/bacnet/heartbeat") {
                    CcuLog.i("HttpServer"," called end point: /bacnet/heartbeat ")
                    updateBacnetHeartBeat();
                }
                post("/watchSub") {
                    CcuLog.i("HttpServer"," watch sub: ")
                    val body = call.receive<String>()
                    if (body != null) {
                        val hGrid = retrieveGridFromRequest(body)
                        val watchSubRequest = CCUHsApi.getInstance().hsClient.watchSubscribe(
                            hGrid
                        )
                        CcuLog.i("HttpServer", "check values in response ${watchSubRequest.isEmpty}")
                        call.respondText(HZincWriter.gridToString(watchSubRequest), ContentType.Any , HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
                post("/watchUnSub") {
                    CcuLog.i("HttpServer"," watch un sub: ")
                    val body = call.receive<String>()
                    if (body != null) {
                        val hGrid = retrieveGridFromRequest(body)
                        val watchSubRequest = CCUHsApi.getInstance().hsClient.watchUnSubscribe(
                            hGrid
                        )
                        CcuLog.i("HttpServer", "check values in response ${watchSubRequest.isEmpty}")
                        call.respondText(HZincWriter.gridToString(watchSubRequest), ContentType.Any , HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
                post("/watchPoll") {
                    val body = call.receive<String>()
                    if (body != null) {
                        val hGrid = retrieveGridFromRequest(body)
                        val watchPollRequest = CCUHsApi.getInstance().hsClient.watchPoll(
                            hGrid
                        )
                        CcuLog.i("HttpServer", "check values in response ${watchPollRequest.isEmpty}")
                        call.respondText(HZincWriter.gridToString(watchPollRequest), ContentType.Any , HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }
    }

    private fun retrieveGridFromRequest(response: String): HGrid {
        val zReader = HZincReader(response)
        return zReader.readGrid()
    }
}