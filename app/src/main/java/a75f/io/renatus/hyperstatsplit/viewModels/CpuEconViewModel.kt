package a75f.io.renatus.hyperstatsplit.viewModels

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitAssociationUtil
import a75f.io.logic.bo.building.hyperstatsplit.profiles.HyperStatSplitProfile
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.CpuEconAnalogOutAssociation
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.CpuEconSensorBusTempAssociation
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconConfiguration
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconProfile
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.renatus.R
import android.app.Application
import android.util.Log

/**
 * Created for HyperStat by Manjunath K on 15-07-2022.
 * Created for HyperStat Split by Nick P on 07-24-2023.
 */

class CpuEconViewModel(application: Application) : HyperStatSplitViewModel(application)  {

    override fun getProfileName(): String {
        return getApplication<Application>().getString(R.string.conventional_package_unit_econ)
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
        hyperStatSplitProfile =  L.getProfile(address) as HyperStatSplitProfile?
        return if (hyperStatSplitProfile != null) {
            hyperStatSplitConfiguration = hyperStatSplitProfile!!.getProfileConfiguration(address)
            val state = ViewState.fromConfigTo(( if (hyperStatSplitProfile !=null ) hyperStatSplitConfiguration!! else HyperStatSplitCpuEconConfiguration()), profileType)
            state
        } else {
            hyperStatSplitProfile = HyperStatSplitCpuEconProfile()
            ViewState.fromConfigTo(HyperStatSplitCpuEconConfiguration(),profileType)
        }
    }


// Save the configuration

    override fun setConfigSelected() {
        // get the state fragment state
        val cpuConfig = currentState.toConfig() as HyperStatSplitCpuEconConfiguration
        cpuConfig.nodeType = nodeType
        cpuConfig.nodeAddress = address
        cpuConfig.priority = ZonePriority.NONE
        addOutputRelayConfigurations(
            cpuConfig.relay1State.enabled, cpuConfig.relay2State.enabled, cpuConfig.relay3State.enabled,
            cpuConfig.relay4State.enabled, cpuConfig.relay5State.enabled, cpuConfig.relay6State.enabled,
            cpuConfig.relay7State.enabled, cpuConfig.relay8State.enabled,
            cpuConfig.analogOut1State.enabled, cpuConfig.analogOut2State.enabled,
            cpuConfig.analogOut3State.enabled, cpuConfig.analogOut4State.enabled,
            cpuConfig
        )
        hyperStatSplitProfile?.profileConfiguration?.put(address, cpuConfig)
        if (hyperStatSplitConfiguration == null) {
            // creating all the Equip and point details for new profile
            hyperStatSplitProfile?.addNewEquip(address, roomName, floorName, cpuConfig)
        } else {
            // update with latest configuration points
            hyperStatSplitProfile?.getHyperStatSplitEquip(address)?.updateConfiguration(cpuConfig)
        }
        // Saving profile details
        L.ccu().zoneProfiles.add(hyperStatSplitProfile)
        L.saveCCUState()
        DesiredTempDisplayMode.setModeType(roomName, CCUHsApi.getInstance())

    }


    override fun getRelayMapping(): Array<String> {
        return getApplication<Application>().resources.getStringArray(R.array.hyperstatsplit_stage_selector)
    }

    override fun getAnalogOutMapping(): Array<String> {
        return getApplication<Application>().resources.getStringArray(R.array.hyperstatsplit_analog_out_selector)
    }

    override fun getUniversalInMapping(): Array<String> {
        return getApplication<Application>().resources.getStringArray(R.array.hyperstatsplit_universal_in_selector)
    }

    override fun isDamperSelected(association: Int): Boolean{
        return (association == CpuEconAnalogOutAssociation.OAO_DAMPER.ordinal)
    }

    override fun isProfileConfigured() :Boolean {
        return hyperStatSplitConfiguration != null
    }

    /*

     */
    override fun validateProfileConfig() : Boolean {

        val cpuConfig = currentState.toConfig() as HyperStatSplitCpuEconConfiguration

        return true

    }

    override fun getValidationMessage() : String {
        return "If OAO Damper is configured, OAT and MAT sensors are required.\n\n" +
                "Enable:\n" +
                "\t- Mixed Air Temperature on the sensor bus\n" +
                "\t- Outside Air Temperature on the sensor bus or a universal input"
    }



}