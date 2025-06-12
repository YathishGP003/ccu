package a75f.io.logic.bo.building.system.client

import a75f.io.logic.bo.building.system.BacnetMstpSubscribeCov
import a75f.io.logic.bo.building.system.BacnetReadRequest
import a75f.io.logic.bo.building.system.BacnetReadRequestMultiple
import a75f.io.logic.bo.building.system.BacnetWhoIsRequest
import a75f.io.logic.bo.building.system.BacnetWriteRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


interface CcuService {

    @POST("/read")
    suspend fun read(@Body readRequest: BacnetReadRequest): Response<ReadResponse>

    @POST("/multiread")
    suspend fun multiread(@Body readRequest: BacnetReadRequestMultiple): Response<MultiReadResponse>

    @POST("/subscribeCov")
    suspend fun subscribeCov(@Body bacnetMstpSubscribeCovRequest: BacnetMstpSubscribeCov): Response<BacnetSubcribeCovResponse>

    @POST("/write")
    suspend fun write(@Body bacnetWriteRequest: BacnetWriteRequest): Response<WriteResponse>

    @POST("/whois")
    suspend fun whois(@Body bacnetWhoIsRequest: BacnetWhoIsRequest): Response<WhoIsResponse>

}