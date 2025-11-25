package a75f.io.logic.bo.building.system.client

import a75f.io.logic.bo.building.system.BacnetIpSubscribeCov
import a75f.io.logic.bo.building.system.BacnetMstpSubscribeCovForAllDevices
import a75f.io.logic.bo.building.system.BacnetReadRequest
import a75f.io.logic.bo.building.system.BacnetReadRequestMultiple
import a75f.io.logic.bo.building.system.BacnetReadRequestMultipleForAllDevices
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

    @POST("/multireadAllDevices")
    suspend fun multireadAllDevices(@Body readRequest: BacnetReadRequestMultipleForAllDevices): Response<MultiReadResponse>

    @POST("/subscribeCovForMstp")
    suspend fun subscribeCovForMstp(@Body bacnetMstpSubscribeCovRequest: BacnetMstpSubscribeCovForAllDevices): Response<BacnetSubcribeCovResponse>

    @POST("/subscribeCovForIp")
    suspend fun subscribeCovForIp(@Body bacnetIpSubscribeCovRequest: BacnetIpSubscribeCov): Response<BacnetSubcribeCovResponse>

    @POST("/write")
    suspend fun write(@Body bacnetWriteRequest: BacnetWriteRequest): Response<WriteResponse>

    @POST("/whois")
    suspend fun whois(@Body bacnetWhoIsRequest: BacnetWhoIsRequest): Response<WhoIsResponse>

}