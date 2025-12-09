package a75f.io.renatus.ui.systemscreen.helper

import a75f.io.logic.L
import a75f.io.logic.bo.building.system.dab.DabAdvancedAhu
import a75f.io.logic.bo.building.system.dab.DabAdvancedHybridRtu
import a75f.io.logic.bo.building.system.dab.DabExternalAhu
import a75f.io.logic.bo.building.system.dab.DabFullyModulatingRtu
import a75f.io.logic.bo.building.system.dab.DabStagedRtu
import a75f.io.logic.bo.building.system.dab.DabStagedRtuWithVfd
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu
import a75f.io.logic.bo.building.system.vav.VavAdvancedHybridRtu
import a75f.io.logic.bo.building.system.vav.VavExternalAhu
import a75f.io.logic.bo.building.system.vav.VavFullyModulatingRtu
import a75f.io.logic.bo.building.system.vav.VavStagedRtu
import a75f.io.logic.bo.building.system.vav.VavStagedRtuWithVfd

fun isDMSupportProfile(): Boolean {
    return (L.ccu().systemProfile is DabExternalAhu
            || (L.ccu().systemProfile is DabStagedRtu && L.ccu().systemProfile !is DabAdvancedHybridRtu)
            || L.ccu().systemProfile is DabStagedRtuWithVfd
            || L.ccu().systemProfile is DabFullyModulatingRtu
            || L.ccu().systemProfile is VavExternalAhu
            || (L.ccu().systemProfile is VavStagedRtu && L.ccu().systemProfile !is VavAdvancedHybridRtu)
            || L.ccu().systemProfile is VavAdvancedAhu
            || L.ccu().systemProfile is DabAdvancedAhu
            || L.ccu().systemProfile is VavStagedRtuWithVfd
            || L.ccu().systemProfile is VavFullyModulatingRtu)
}