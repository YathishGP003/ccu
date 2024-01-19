package a75f.io.renatus.ENGG.bacnet.services.client

import a75f.io.renatus.ENGG.bacnet.services.BacnetReadRequest
import a75f.io.renatus.ENGG.bacnet.services.BacnetReadRequestMultiple
import a75f.io.renatus.ENGG.bacnet.services.BacnetWhoIsRequest
import a75f.io.renatus.ENGG.bacnet.services.BacnetWriteRequest
import a75f.io.renatus.ENGG.bacnet.services.MultiReadResponse
import a75f.io.renatus.ENGG.bacnet.services.ReadResponse
import a75f.io.renatus.ENGG.bacnet.services.WhoIsResponse
import a75f.io.renatus.ENGG.bacnet.services.WriteResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


interface CcuService {

    @POST("/read")
    suspend fun read(@Body readRequest: BacnetReadRequest): Response<ReadResponse>

    @POST("/multiread")
    suspend fun multiread(@Body readRequest: BacnetReadRequestMultiple): Response<MultiReadResponse>

    @POST("/write")
    suspend fun write(@Body bacnetWriteRequest: BacnetWriteRequest): Response<WriteResponse>

    @POST("/whois")
    suspend fun whois(@Body bacnetWhoIsRequest: BacnetWhoIsRequest): Response<WhoIsResponse>

}