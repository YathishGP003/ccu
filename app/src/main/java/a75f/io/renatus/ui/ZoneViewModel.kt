package a75f.io.renatus.ui

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.observer.PointSubscriber
import a75f.io.renatus.ui.tempprofiles.updateDesiredCurrentTemp
import a75f.io.renatus.util.HeartBeatUtil
import a75f.io.renatus.util.SeekArc
import android.annotation.SuppressLint
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("StaticFieldLeak")
class ZoneViewModel: ViewModel(), PointSubscriber {
    private var job: Job? = null
    lateinit var equip: Equip
    lateinit var roomId: String
    var seekArc : SeekArc? = null
    var heartbeatView: View? = null
    var isExternalEquip = false


    companion object {
        private var zoneViewModel: ZoneViewModel? = null

        fun create(roomId: String): ZoneViewModel {
            zoneViewModel?.stopObservingZoneHealth()
            zoneViewModel = null

            zoneViewModel = ZoneViewModel().apply {
                this.roomId = roomId
            }

            return zoneViewModel!!
        }
    }

    fun observeZoneHealth() {
        stopObservingZoneHealth()

        job = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {

                val equips = CCUHsApi.getInstance()
                    .readAllEntities("equip and zone and roomRef ==\"$roomId\"")

                val isZoneAlive = HeartBeatUtil.isZoneAlive(equips)

                withContext(Dispatchers.Main) {
                    HeartBeatUtil.zoneStatus(heartbeatView, isZoneAlive)
                    if(!isExternalEquip) updateSeekArc()
                }

                delay(60_000L)
            }
        }
    }

    fun stopObservingZoneHealth() {
        job?.cancel()
        job = null
    }

    override fun onCleared() {
        super.onCleared()
        stopObservingZoneHealth()
        heartbeatView = null
    }

    /*
    * Update SeekArc based on current temp and desired temp
     */
    private fun updateSeekArc() {
        try {
            updateDesiredCurrentTemp(equip, seekArc!!)
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }


    fun subScribeHeartBeatPoint() {}

    fun unSubScribeHeartBeatPoint() {}

    override fun onHisPointChanged(pointId: String, value: Double) {}

    override fun onWritablePointChanged(pointId: String, value: Any) {}

}