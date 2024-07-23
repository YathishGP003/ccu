package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader.getModelForDomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.vav.AcbProfileConfiguration
import a75f.io.logic.bo.building.vav.VavAcbProfile
import com.google.gson.JsonObject

class ACBRelayConfigHandler {
    companion object {
        fun updateACBRelay1Type(msgObject: JsonObject, configPoint: Point, hayStack: CCUHsApi) {
            val value = msgObject.get("value").asDouble
            if (value == CCUHsApi.getInstance().readDefaultValById(configPoint.id)) {
                CcuLog.d(L.TAG_CCU_PUBNUB, "updateVAVConfigPoint - Message is not handled")
                return
            }

            val address : Short = configPoint.group.toShort()
            val profile = L.getProfile(address) as VavAcbProfile
            val equip = profile.equip
            var config = profile.domainProfileConfiguration as AcbProfileConfiguration
            config.relay1Association.associationVal = value.toInt()

            val equipBuilder = ProfileEquipBuilder(hayStack)
            equipBuilder.updateEquipAndPoints(
                    config,
                    getModelForDomainName(equip.getDomainName()),
                    equip.getSiteRef(),
                    equip.getDisplayName(), true
            )
        }
    }
}