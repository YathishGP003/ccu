package a75f.io.domain.util
import io.seventyfivef.domainmodeler.client.ModelDirective

/**
 * Reading of Model through ModelLoader will have the model cached after first read.
 */
object ModelLoader {

    fun getBuildingEquipModelDef(): ModelDirective {
        return ModelCache.getModelById(MODEL_BUILDING_EQUIP)
    }

    fun getSmartNodeVavNoFanModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_SN_VAV_NO_FAN)
    }

    fun getSmartNodeVavSeriesModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_SN_VAV_SERIES_FAN)
    }

    fun getSmartNodeVavParallelFanModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_SN_VAV_PARALLEL_FAN)
    }

    fun getSmartNodeVavAcbModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_SN_VAV_ACB)
    }

    fun getSmartNodeBypassDamperModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_SN_BYPASS_DAMPER)
    }

    fun getSmartNodeDevice() : ModelDirective {
        return ModelCache.getModelById(MODEL_SMART_NODE_DEVICE)
    }

    fun getHelioNodeVavNoFanModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_HN_VAV_NO_FAN)
    }

    fun getHelioNodeVavSeriesModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_HN_VAV_SERIES_FAN)
    }

    fun getHelioNodeVavParallelFanModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_HN_VAV_PARALLEL_FAN)
    }

    fun getHelioNodeVavAcbModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_HN_VAV_ACB)
    }

    fun getHelioNodeDevice() : ModelDirective {
        return ModelCache.getModelById(MODEL_HELIO_NODE_DEVICE)
    }

    fun getCMDeviceModel() : ModelDirective {
        return ModelCache.getModelById(MODEL_CM_DEVICE)
    }
    fun getVavStageRtuModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_VAV_STAGED_RTU)
    }

    fun getVavStagedVfdRtuModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_VAV_STAGED_VFD_RTU)
    }
    fun getDabExternalAhuModel() : ModelDirective {
        return ModelCache.getModelById(MODEL_EXTERNAL_AHU_DAB)
    }
    fun getVavExternalAhuModel() : ModelDirective {
        return ModelCache.getModelById(MODEL_EXTERNAL_AHU_VAV)
    }
    fun getVavModulatingRtuModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_VAV_MODULATING_AHU)
    }
    fun getModelForDomainName( domainName : String) : ModelDirective {
        return when(domainName) {
            "smartnodeVAVReheatNoFan" -> getSmartNodeVavNoFanModelDef()
            "smartnodeVAVReheatSeriesFan" -> getSmartNodeVavSeriesModelDef()
            "smartnodeVAVReheatParallelFan" -> getSmartNodeVavParallelFanModelDef()
            "smartnodeBypassDamper" -> getSmartNodeBypassDamperModelDef()
            "smartnodeActiveChilledBeam" -> getSmartNodeVavAcbModelDef()
            "helionodeVAVReheatNoFan" -> getHelioNodeVavNoFanModelDef()
            "helionodeVAVReheatSeriesFan" -> getHelioNodeVavSeriesModelDef()
            "helionodeVAVReheatParallelFan" -> getHelioNodeVavParallelFanModelDef()
            "helionodeActiveChilledBeam" -> getHelioNodeVavAcbModelDef()
            else -> throw IllegalStateException("Invalid Model Name")
        }
    }
}