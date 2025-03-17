package a75f.io.domain.migration

import a75f.io.domain.util.MODEL_BUILDING_EQUIP
import a75f.io.domain.util.MODEL_CCU_BASE_CONFIGURATION
import a75f.io.domain.util.MODEL_CCU_DIAG_EQUIP
import a75f.io.domain.util.MODEL_CM_DEVICE
import a75f.io.domain.util.MODEL_CONNECT_DEVICE
import a75f.io.domain.util.MODEL_DAB_ADVANCED_AHU_V2_CM
import a75f.io.domain.util.MODEL_DAB_ADVANCED_AHU_V2_CONNECT
import a75f.io.domain.util.MODEL_DAB_MODULATING_AHU
import a75f.io.domain.util.MODEL_DAB_STAGED_RTU
import a75f.io.domain.util.MODEL_DAB_STAGED_VFD_RTU
import a75f.io.domain.util.MODEL_EXTERNAL_AHU_DAB
import a75f.io.domain.util.MODEL_EXTERNAL_AHU_VAV
import a75f.io.domain.util.MODEL_HELIONODE_PID
import a75f.io.domain.util.MODEL_HELIO_NODE_DAB
import a75f.io.domain.util.MODEL_HELIO_NODE_DEVICE
import a75f.io.domain.util.MODEL_HELIO_NODE_SSE
import a75f.io.domain.util.MODEL_HN_VAV_ACB
import a75f.io.domain.util.MODEL_HN_VAV_NO_FAN
import a75f.io.domain.util.MODEL_HN_VAV_PARALLEL_FAN
import a75f.io.domain.util.MODEL_HN_VAV_SERIES_FAN
import a75f.io.domain.util.MODEL_HYPERSTAT_CPU
import a75f.io.domain.util.MODEL_HYPERSTAT_DEVICE
import a75f.io.domain.util.MODEL_HYPERSTAT_MONITORING
import a75f.io.domain.util.MODEL_HYPERSTAT_HPU
import a75f.io.domain.util.MODEL_HYPERSTAT_PIPE2
import a75f.io.domain.util.MODEL_HYPERSTAT_SPLIT_CPU
import a75f.io.domain.util.MODEL_HYPERSTAT_SPLIT_DEVICE
import a75f.io.domain.util.MODEL_MYSTAT_CPU
import a75f.io.domain.util.MODEL_MYSTAT_DEVICE
import a75f.io.domain.util.MODEL_MYSTAT_HPU
import a75f.io.domain.util.MODEL_MYSTAT_PIPE2
import a75f.io.domain.util.MODEL_OTN_DEVICE
import a75f.io.domain.util.MODEL_OTN_TI
import a75f.io.domain.util.MODEL_SMARTNODE_PID
import a75f.io.domain.util.MODEL_SMART_NODE_DAB
import a75f.io.domain.util.MODEL_SMART_NODE_DEVICE
import a75f.io.domain.util.MODEL_SMART_NODE_SSE
import a75f.io.domain.util.MODEL_SN_BYPASS_DAMPER
import a75f.io.domain.util.MODEL_SN_OAO
import a75f.io.domain.util.MODEL_SN_VAV_ACB
import a75f.io.domain.util.MODEL_SN_VAV_NO_FAN
import a75f.io.domain.util.MODEL_SN_VAV_PARALLEL_FAN
import a75f.io.domain.util.MODEL_SN_VAV_SERIES_FAN
import a75f.io.domain.util.MODEL_TI
import a75f.io.domain.util.MODEL_TI_DEVICE
import a75f.io.domain.util.MODEL_VAV_ADVANCED_AHU_V2_CM
import a75f.io.domain.util.MODEL_VAV_ADVANCED_AHU_V2_CONNECT
import a75f.io.domain.util.MODEL_VAV_MODULATING_AHU
import a75f.io.domain.util.MODEL_VAV_STAGED_RTU
import a75f.io.domain.util.MODEL_VAV_STAGED_VFD_RTU
import io.seventyfivef.domainmodeler.common.Version

/**
 * Created by Manjunath K on 23-06-2023.
 */

data class ModelMeta(val modelId: String,val version: Version)
const val MAJOR = "major"
const val MINOR = "minor"
const val PATCH = "patch"
const val ID = "id"
const val MODEL_VERSION = "version"

fun getRequiredModels(): List<String> {
    return listOf(
            MODEL_BUILDING_EQUIP,
            MODEL_SN_VAV_NO_FAN,
            MODEL_SN_VAV_SERIES_FAN,
            MODEL_SN_VAV_PARALLEL_FAN,
            MODEL_HN_VAV_NO_FAN,
            MODEL_HN_VAV_SERIES_FAN,
            MODEL_HN_VAV_PARALLEL_FAN,
            MODEL_SN_VAV_ACB,
            MODEL_HN_VAV_ACB,
            MODEL_HYPERSTAT_SPLIT_CPU,
            MODEL_SN_BYPASS_DAMPER,
            MODEL_SMART_NODE_DEVICE,
            MODEL_HELIO_NODE_DEVICE,
            MODEL_HYPERSTAT_SPLIT_DEVICE,
            MODEL_EXTERNAL_AHU_DAB,
            MODEL_EXTERNAL_AHU_VAV,
            MODEL_VAV_STAGED_RTU,
            MODEL_VAV_STAGED_VFD_RTU,
            MODEL_VAV_MODULATING_AHU,
            MODEL_EXTERNAL_AHU_VAV,
            MODEL_DAB_STAGED_RTU,
            MODEL_DAB_STAGED_VFD_RTU,
            MODEL_SMART_NODE_DAB,
            MODEL_HELIO_NODE_DAB,
            MODEL_DAB_MODULATING_AHU,
            MODEL_VAV_ADVANCED_AHU_V2_CM,
            MODEL_VAV_ADVANCED_AHU_V2_CONNECT,
            MODEL_DAB_ADVANCED_AHU_V2_CM,
            MODEL_DAB_ADVANCED_AHU_V2_CONNECT,
            MODEL_CM_DEVICE,
            MODEL_CONNECT_DEVICE,
            MODEL_HYPERSTAT_DEVICE,
            MODEL_HYPERSTAT_PIPE2,
            MODEL_SN_OAO,
            MODEL_SMART_NODE_SSE,
            MODEL_HELIO_NODE_SSE,
            MODEL_HYPERSTAT_CPU,
            MODEL_HYPERSTAT_HPU,
            MODEL_HYPERSTAT_MONITORING,
            MODEL_CCU_DIAG_EQUIP,
            MODEL_CCU_BASE_CONFIGURATION,
            MODEL_TI_DEVICE,
            MODEL_TI,
            MODEL_OTN_TI,
            MODEL_OTN_DEVICE,
            MODEL_HELIONODE_PID,
            MODEL_SMARTNODE_PID,
            MODEL_MYSTAT_DEVICE,
            MODEL_MYSTAT_CPU,
            MODEL_MYSTAT_PIPE2,
            MODEL_MYSTAT_HPU
    )
}