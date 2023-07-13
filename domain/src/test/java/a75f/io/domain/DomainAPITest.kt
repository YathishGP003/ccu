package a75f.io.domain

import a75f.io.domain.service.DomainService
import a75f.io.domain.service.ResponseCallback
import org.junit.Test

/**
 * Created by Manjunath K on 13-07-2023.
 */

class DomainAPITest{

    @Test
    fun testApi() {
        val service = DomainService()
        service.readModbusModelsList("emr", object : ResponseCallback {
            override fun onSuccessResponse(response: String?) {
                println(" ================================$response")
            }
            override fun onErrorResponse(response: String?) {
                println(" ================================$response")
            }
        })
    }
}