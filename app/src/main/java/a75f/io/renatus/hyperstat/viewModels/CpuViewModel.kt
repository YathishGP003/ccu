package a75f.io.renatus.hyperstat.viewModels

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstat.profiles.HyperStatProfile
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.CpuAnalogOutAssociation
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.HyperStatCpuConfiguration
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.HyperStatCpuProfile
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconConfiguration
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconProfile
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.renatus.R
import android.app.Application
import android.util.Log

/**
 * Created by Manjunath K on 15-07-2022.
 */

class CpuViewModel(application: Application) : HyperStatViewModel(application)  {

    override fun getProfileName(): String {
        return getApplication<Application>().getString(R.string.conventional_package_unit)
    }

    override fun initData(address: Short, roomName: String, floorName: String, nodeType: NodeType, profileType: ProfileType) {
        this.address = address
        this.roomName = roomName
        this.floorName = floorName
        this.nodeType = nodeType
        this.profileType = profileType
        viewState.onNext(initialViewState(address))
    }


    private fun initialViewState(address: Short): ViewState {
        hyperStatProfile =  L.getProfile(address) as HyperStatProfile?
        return if (hyperStatProfile != null) {
            hyperStatConfiguration = hyperStatProfile!!.getProfileConfiguration(address)
            ViewState.fromConfigTo(( if (hyperStatProfile !=null ) hyperStatConfiguration!! else HyperStatCpuConfiguration()), profileType)
        } else {
            hyperStatProfile = HyperStatCpuProfile()
            ViewState.fromConfigTo(HyperStatCpuConfiguration(),profileType)
        }
    }


// Save the configuration

    override fun setConfigSelected() {

        // get the state fragment state
        val cpuConfig = currentState.toConfig() as HyperStatCpuConfiguration

        cpuConfig.nodeType = nodeType
        cpuConfig.nodeAddress = address
        cpuConfig.priority = ZonePriority.NONE

        addOutputRelayConfigurations(
            cpuConfig.relay1State.enabled, cpuConfig.relay2State.enabled, cpuConfig.relay3State.enabled,
            cpuConfig.relay4State.enabled, cpuConfig.relay5State.enabled, cpuConfig.relay6State.enabled,
            cpuConfig.analogOut1State.enabled, cpuConfig.analogOut2State.enabled, cpuConfig.analogOut3State.enabled,
            cpuConfig
        )

        hyperStatProfile?.profileConfiguration?.put(address, cpuConfig)

        if (hyperStatConfiguration == null) {
            // creating all the Equip and point details for new profile
            hyperStatProfile?.addNewEquip(address, roomName, floorName, cpuConfig)
        } else {
            // update with latest configuration points
            hyperStatProfile?.getHyperStatEquip(address)?.updateConfiguration(cpuConfig)
        }

        // Saving profile details
        L.ccu().zoneProfiles.add(hyperStatProfile)
        L.saveCCUState()
        DesiredTempDisplayMode.setModeType(roomName, CCUHsApi.getInstance())

    }

    override fun getRelayMapping(): Array<String> {
        return getApplication<Application>().resources.getStringArray(R.array.hyperstat_stage_selector)

    }

    override fun getAnalogOutMapping(): Array<String> {
        return getApplication<Application>().resources.getStringArray(R.array.hyperstat_analog_out_selector)
    }

    override fun isDamperSelected(association: Int): Boolean{
        return (association == CpuAnalogOutAssociation.DCV_DAMPER.ordinal)
    }

    override fun isProfileConfigured() :Boolean {
        return hyperStatConfiguration != null
    }
}