package a75f.io.domain.equips

open class DomainEquip(val equipRef : String) {
    fun getId() : String {
        return equipRef
    }

    /**
     * This is a map of all the controllers in the equip.
     * The key is the controller name and the value is the controller object.
     */
    val controllers = mutableMapOf<String,Any>()
}