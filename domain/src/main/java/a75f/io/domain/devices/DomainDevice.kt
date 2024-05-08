package a75f.io.domain.devices

open class DomainDevice(val deviceRef : String) {
    fun getId() : String {
        return deviceRef
    }
}