package a75f.io.domain.util

import a75f.io.domain.api.DomainName

object CommonQueries {
    const val SYSTEM_PROFILE = "system and equip and not modbus and not connectModule and not bacnet"
    const val BACNET_EQUIP_ZONE ="equip and bacnet and zone"
    const val UNOCCUPIED_SET_BACK_ZONE = "unoccupied and zone and setback and schedulable"
    const val MYSTAT_EQUIP = "domainName ==\"${DomainName.myStatCPU}\" or domainName ==\"${DomainName.myStatHPU}\" or domainName ==\"${DomainName.mystat2PFCU}\" or domainName ==\"${DomainName.mystat4PFCU}\""
    const val HYPERSTAT_EQUIP = "domainName ==\"${DomainName.hyperstatCPU}\" or domainName ==\"${DomainName.hyperstatHPU}\" or domainName ==\"${DomainName.hyperstat2PFCU}\""
    const val HYPERSTATSPLIT_UNIT_VENTILATOR_EQUIP = "domainName ==\"${DomainName.hyperstatSplitCPU}\" or domainName ==\"${DomainName.hyperstatSplit4PEcon}\" or domainName ==\"${DomainName.hyperstatSplit2PEcon}\"";
    const val ALL_DM_STANDLONE_EQUIP = "$MYSTAT_EQUIP or $HYPERSTAT_EQUIP or $HYPERSTATSPLIT_UNIT_VENTILATOR_EQUIP"

}