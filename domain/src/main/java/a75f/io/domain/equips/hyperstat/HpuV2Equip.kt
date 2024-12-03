package a75f.io.domain.equips.hyperstat

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

/**
 * Created by Manjunath K on 26-09-2024.
 */

class HpuV2Equip(equipRef : String) : HyperStatEquip(equipRef){
    val compressorLoopOutput = Point("", equipRef)
}