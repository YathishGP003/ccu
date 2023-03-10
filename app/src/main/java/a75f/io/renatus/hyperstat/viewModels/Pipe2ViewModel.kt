package a75f.io.renatus.hyperstat.viewModels

import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstat.profiles.HyperStatProfile
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.HyperStatPipe2Configuration
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.HyperStatPipe2Profile
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.Pipe2AnalogOutAssociation
import a75f.io.renatus.R
import android.app.Application

/**
 * Created by Manjunath K on 08-08-2022.
 */

class Pipe2ViewModel(application: Application) : HyperStatViewModel(application) {
    override fun getProfileName(): String {
        return  return getApplication<Application>().getString(R.string.two_pipe_fcu)
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
            ViewState.fromConfigTo(( if (hyperStatProfile !=null ) hyperStatConfiguration!! else HyperStatPipe2Configuration()), profileType)

        } else {
            hyperStatProfile = HyperStatPipe2Profile()
            ViewState.fromConfigTo(HyperStatPipe2Configuration(),profileType)
        }
 }


// Save the configuration

    override fun setConfigSelected() {

        // get the state fragment state
        val pipe2Config = currentState.toConfig() as HyperStatPipe2Configuration

        pipe2Config.nodeType = nodeType
        pipe2Config.nodeAddress = address
        pipe2Config.priority = ZonePriority.NONE

        addOutputRelayConfigurations(
            pipe2Config.relay1State.enabled, pipe2Config.relay2State.enabled, pipe2Config.relay3State.enabled,
            pipe2Config.relay4State.enabled, pipe2Config.relay5State.enabled, pipe2Config.relay6State.enabled,
            pipe2Config.analogOut1State.enabled, pipe2Config.analogOut2State.enabled, pipe2Config.analogOut3State.enabled,
            pipe2Config
        )

        hyperStatProfile?.profileConfiguration?.put(address, pipe2Config)

        if (hyperStatConfiguration == null) {
            // creating all the Equip and point details for new profile
            hyperStatProfile?.addNewEquip(address, roomName, floorName, pipe2Config)
        } else {
            // update with latest configuration points
            hyperStatProfile?.getHyperStatEquip(address)?.updateConfiguration(pipe2Config)
        }

        // Saving profile details
        L.ccu().zoneProfiles.add(hyperStatProfile)
        L.saveCCUState()

    }

    override fun getRelayMapping(): Array<String> {
        return getApplication<Application>().resources.getStringArray(R.array.hyperstat_2pipe_stage_selector)
    }

    override fun getAnalogOutMapping(): Array<String> {
        return getApplication<Application>().resources.getStringArray(R.array.hyperstat_pipe2_analog_out_selector)
    }

    override fun th2SwitchChanged(checked: Boolean) {
    // For 2 pipe profile we should not allow user to operate the toggle
    // viewState.onNext(currentState.copy(th2Enabled = checked ))
    }

    override fun getTh2SensorLabel(): String {
        return getApplication<Application>().getString(R.string.supply_water_sensor)
    }

    override fun isDamperSelected(association: Int): Boolean{
        return (association == Pipe2AnalogOutAssociation.DCV_DAMPER.ordinal)
    }

    override fun isProfileConfigured() :Boolean {
        return hyperStatConfiguration != null
    }
}