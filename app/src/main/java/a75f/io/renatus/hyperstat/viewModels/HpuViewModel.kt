package a75f.io.renatus.hyperstat.viewModels

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstat.profiles.HyperStatProfile
import a75f.io.logic.bo.building.hyperstat.profiles.hpu.HyperStatHpuConfiguration
import a75f.io.logic.bo.building.hyperstat.profiles.hpu.HyperStatHpuProfile
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.renatus.R
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

/**
 * Created by Manjunath K on 28-12-2022.
 */

class HpuViewModel(application: Application) : HyperStatViewModel(application) {

    override fun getProfileName(): String {
        return getApplication<Application>().getString(R.string.heat_pump_unit)
    }

    override fun isProfileConfigured() :Boolean {
        return hyperStatConfiguration != null
    }

    override fun initData(address: Short, roomName: String, floorName: String, nodeType: NodeType, profileType: ProfileType) {
        this.address = address
        this.roomName = roomName
        this.floorName = floorName
        this.nodeType = nodeType
        this.profileType = profileType
        viewState.onNext(initialViewState(address))
    }

    override fun getRelayMapping(): Array<String> {
        return getApplication<Application>().resources.getStringArray(R.array.hyperstat_hpu_stage_selector)
    }

    override fun getAnalogOutMapping(): Array<String> {
        return getApplication<Application>().resources.getStringArray(R.array.hyperstat_hpu_analog_out_selector)
    }

    override fun isDamperSelected(association: Int): Boolean {
        return true
    }

    override fun setConfigSelected() {

        // get the state fragment state
        val hupConfig = currentState.toConfig() as HyperStatHpuConfiguration

        hupConfig.nodeType = nodeType
        hupConfig.nodeAddress = address
        hupConfig.priority = ZonePriority.NONE

        addOutputRelayConfigurations(
            hupConfig.relay1State.enabled, hupConfig.relay2State.enabled, hupConfig.relay3State.enabled,
            hupConfig.relay4State.enabled, hupConfig.relay5State.enabled, hupConfig.relay6State.enabled,
            hupConfig.analogOut1State.enabled, hupConfig.analogOut2State.enabled, hupConfig.analogOut3State.enabled,
            hupConfig
        )

        hyperStatProfile?.profileConfiguration?.put(address, hupConfig)

        if (hyperStatConfiguration == null) {
            // creating all the Equip and point details for new profile
            hyperStatProfile?.addNewEquip(address, roomName, floorName, hupConfig)
        } else {
            // update with latest configuration points
            hyperStatProfile?.getHyperStatEquip(address)?.updateConfiguration(hupConfig)
        }

        // Saving profile details
        L.ccu().zoneProfiles.add(hyperStatProfile)
        L.saveCCUState()
        DesiredTempDisplayMode.setModeType(roomName, CCUHsApi.getInstance())
    }

    private fun initialViewState(address: Short): ViewState {

        hyperStatProfile =  L.getProfile(address) as HyperStatProfile?
        return if (hyperStatProfile != null) {

            hyperStatConfiguration = hyperStatProfile!!.getProfileConfiguration(address)
            ViewState.fromConfigTo(( if (hyperStatProfile !=null ) hyperStatConfiguration!! else HyperStatHpuConfiguration()), profileType)

        } else {
            hyperStatProfile = HyperStatHpuProfile()
            ViewState.fromConfigTo(HyperStatHpuConfiguration(),profileType)
        }
    }

    override fun getRelayMappingAdapter(context: Context, values: Array<String>): ArrayAdapter<*> {

        var typeOEnabled = true
        var typeBEnabled = true

        val adapter:ArrayAdapter<String> = object: ArrayAdapter<String>(
            context,
            R.layout.spinner_dropdown_item,
            values
        ){
            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {

                val view:TextView = super.getDropDownView(
                    position,
                    convertView,
                    parent
                ) as TextView
                if (parent.tag != null) {
                    if (shouldDisableRelayChangeOver(position, parent.tag.toString().toInt())) {
                        view.setTextColor(Color.LTGRAY)
                        when (position) {
                            TYPE_O_INDEX -> typeOEnabled = false
                            TYPE_B_INDEX -> typeBEnabled = false
                        }
                    } else {
                        view.setTextColor(Color.BLACK)
                        when (position) {
                            TYPE_O_INDEX -> typeOEnabled = true
                            TYPE_B_INDEX -> typeBEnabled = true
                        }
                    }
                } else {
                    if ((position == TYPE_O_INDEX && !typeOEnabled) || (position == TYPE_B_INDEX && !typeBEnabled)) {
                        view.setTextColor(Color.LTGRAY)
                    } else {
                        view.setTextColor(Color.BLACK)
                    }
                }
                view.setBackgroundResource(R.drawable.custmspinner)
                return view
            }

            override fun isEnabled(position: Int): Boolean {
                return when (position) {
                    TYPE_O_INDEX -> typeOEnabled
                    TYPE_B_INDEX -> typeBEnabled
                    else -> true
                }
            }
        }
        return adapter
    }

    private fun shouldDisableRelayChangeOver(selectedPos : Int, relayPos : Int) : Boolean {
        if (selectedPos != TYPE_O_INDEX && selectedPos != TYPE_B_INDEX) {
            return false
        }
        val typeBSelections = currentState.relays.count{ it.enabled && it.association == TYPE_B_INDEX}
        val typeOSelections = currentState.relays.count{ it.enabled && it.association == TYPE_O_INDEX }

        when (selectedPos) {
            TYPE_O_INDEX -> {
                if (typeOSelections > 0) {
                    return false
                } else if (typeBSelections > 0){
                    val typeBSelectedRelay = currentState.relays.find{ it.enabled && it.association == TYPE_B_INDEX }
                    return typeBSelections > 1 || currentState.relays.indexOf(typeBSelectedRelay) != relayPos
                }
            }
            TYPE_B_INDEX -> {
                if (typeBSelections > 0) {
                    return false
                } else if (typeOSelections > 0) {
                    val typeOSelectedRelay = currentState.relays.find{ it.enabled && it.association== TYPE_O_INDEX}
                    return typeOSelections > 1 || currentState.relays.indexOf(typeOSelectedRelay) != relayPos
                }
            }
        }
        return false
    }

    override fun validateProfileConfig(): Boolean {
        return isChangeOverRelaySelected() && hasOnlyOneChangeoverConfigsSelected()
    }

    override fun getValidationMessage(): String {
        if (!isChangeOverRelaySelected()) {
            return "Heatpump cannot be configured without enabling relay for ChangeOver valve"
        } else if (!hasOnlyOneChangeoverConfigsSelected()) {
            return "Please select same type of ChangeOver valve for all relays"
        }
        return ""
    }
    private fun isChangeOverRelaySelected() : Boolean {
        currentState.relays.forEach { if (it.enabled && (it.association == TYPE_O_INDEX || it.association == TYPE_B_INDEX)) {
                return true
            }
        }
        return false
    }

    private fun hasOnlyOneChangeoverConfigsSelected() : Boolean {
        val typeBSelections = currentState.relays.count{ it.enabled && it.association == TYPE_B_INDEX}
        val typeOSelections = currentState.relays.count{ it.enabled && it.association == TYPE_O_INDEX }
        return !(typeBSelections > 0  && typeOSelections > 0)
    }

    companion object {
        const val TYPE_O_INDEX = 12
        const val TYPE_B_INDEX = 13
    }
}