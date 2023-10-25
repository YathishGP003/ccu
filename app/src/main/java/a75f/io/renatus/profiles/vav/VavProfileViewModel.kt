package a75f.io.renatus.profiles.vav

import a75f.io.domain.util.ModelSource
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.definitions.DamperShape
import a75f.io.logic.bo.building.definitions.DamperType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.definitions.ReheatType
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.modbus.util.ModbusLevel
import android.os.Bundle
import androidx.lifecycle.ViewModel
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlin.properties.Delegates

class VavProfileViewModel : ViewModel() {



    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var profileType: ProfileType
    private var deviceAddress by Delegates.notNull<Short>()

    lateinit var profileConfiguration: VavProfileConfiguration

    val damperTypes = DamperType.values().map { it.displayName }
    val damperSizes = (2..22 step 2).map { it.toString() }
    val reheatTypes = mutableListOf("Not Installed") + ReheatType.values().map { it.displayName }
    val zonePriority = ZonePriority.values().map { it.name }
    val damperShape = DamperShape.values().map { it.displayName }

    private lateinit var model : SeventyFiveFProfileDirective

    fun init(bundle: Bundle) {
        deviceAddress = bundle.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
        zoneRef = bundle.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = bundle.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        val profileOriginalValue = bundle.getInt(FragmentCommonBundleArgs.PROFILE_TYPE)
        profileType = ProfileType.values()[profileOriginalValue]
        model = ModelSource.getModelByProfileName("smartnodeVAVReheatNoFan") as SeventyFiveFProfileDirective
        profileConfiguration = VavProfileConfiguration(deviceAddress.toInt(), NodeType.SMART_NODE.name, 0,
            zoneRef, floorRef , model ).getDefaultConfiguration()
        CcuLog.i("CCU_DOMAIN"," offset "+profileConfiguration.temperatureOffset)
    }
}