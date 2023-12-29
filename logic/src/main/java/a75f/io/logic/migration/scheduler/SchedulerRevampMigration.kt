package a75f.io.logic.migration.scheduler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.util.validateMigration
import a75f.io.data.message.MessageDao
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.migration.Migration
import a75f.io.logic.migration.ServiceGenerator
import a75f.io.logic.migration.schedulerevamp.handleMessage
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class SchedulerRevampMigration(hsApi : CCUHsApi) : Migration {
    override val hayStack = hsApi
    override fun isMigrationRequired(): Boolean {
        //Need better logic here that guarantees all the migration is done.
        //return true
        return !(validateMigration())
    }

    override fun doMigration() {
        CcuLog.d(L.TAG_CCU_SCHEDULER, "Do SchedulerRevampMigration")
        val migrationService = ServiceGenerator().createService(hayStack.gatewayServiceUrl, hayStack.jwt)

        hayStack.site?.id?.let {
            val call =migrationService.triggerSchedulerMigration(it.replace("@",""))
            call.enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    CcuLog.d(L.TAG_CCU_SCHEDULER, "Schedule migration failure:  ${t.message}")
                }

                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if(response.isSuccessful){
                        CcuLog.d(L.TAG_CCU_SCHEDULER, "Schedule migration isSuccessful:  $response")
                    }else {
                        if(response.code() == 405 ) {
                            val responseString = response.errorBody()!!.string()
                            if ((responseString.substringAfter("Status").contains("Success", true)))
                                handleMessage()
                        }else{
                            CcuLog.d(
                                L.TAG_CCU_SCHEDULER,
                                "error while Schedule migration new CCU:  $response"
                            )
                        }

                    }
                }

            })
        }
    }
}

