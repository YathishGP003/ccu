package a75f.io.device.daikin

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.simpleframework.xml.Element
import org.simpleframework.xml.Root
import retrofit2.Response
import retrofit2.http.*


interface IEService {

    @Headers(value = ["Accept: application/xml",
        "Content-type:text/plain"])
    @PUT("/BN/MT3/{pointType}/{pointName}/Present_Value?resp-format=eXML")
    fun writePoint(
        @Path("pointType") pointType: String,
        @Path("pointName") pointName: String,
        @Body pointVal : String
    ): Observable<Response<Void>>

    @Headers(value = ["Accept: application/xml",
        "Content-type:text/plain"])
    @GET("/BN/MT3/{pointType}/{pointName}/Present_Value?resp-format=eXML")
    fun readPoint(
        @Path("pointType") pointType: String,
        @Path("pointName") pointName: String
    ): Single<Results>

}
//TODO- Exact response structure from Daikin IE is unknown now. This will be changed after field trials.

@Root(name = "results")
data class Results @JvmOverloads constructor(
    @field:Element(name = "result")
    var result: String? = null
)