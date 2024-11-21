package a75f.io.domain.devices

import a75f.io.api.haystack.util.hayStack
import a75f.io.constants.CcuFieldConstants

class CCUDevice (deviceRef : String) : DomainDevice (deviceRef)  {
    /*After completion of replace ccu, we build domain [from Globals class]
    * we were not able to get ccu entity by deviceRef so using query*/
    private val ccuDeviceMap = hayStack.readEntity("ccu and device")
    val isCCUExists = ccuDeviceMap.isNotEmpty()
    val ccuDisName = ccuDeviceMap[CcuFieldConstants.DESCRIPTION].toString()
    val installerEmail = ccuDeviceMap[CcuFieldConstants.INSTALLER_EMAIL].toString()
    val managerEmail = ccuDeviceMap[CcuFieldConstants.FACILITY_MANAGER_EMAIL].toString()
    val siteRef = ccuDeviceMap[CcuFieldConstants.SITEREF].toString()
    val equipRef = ccuDeviceMap[CcuFieldConstants.EQUIPREF].toString()
    val gatewayRef = ccuDeviceMap[CcuFieldConstants.GATEWAYREF].toString()
    val ahuRef = ccuDeviceMap[CcuFieldConstants.AHUREF].toString()
}