package a75f.io.device.daikin

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path


interface IEService {

    @PUT("/BN/MT3/{pointType}/{pointName}/Present_Value?resp-format=eXML&access-token=123456789")
    fun writePoint(
        @Path("pointType") pointType: String,
        @Path("pointName") pointName: String,
        @Body pointVal : String
    ): Completable

    @GET("/BN/MT3/{pointType}/{pointName}/Present_Value?resp-format=eXML&access-token=123456789")
    fun readPoint(
        @Path("pointType") pointType: String,
        @Path("pointName") pointName: String
    ): Single<IEResponse>

}
//TODO- Exact response structure from Daikin IE is unknown now. This will be changed after field trials.
data class IEResponse(
    val responseVal: String
)
