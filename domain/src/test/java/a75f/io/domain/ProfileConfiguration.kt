package a75f.io.domain

abstract class ProfileConfiguration (nodeAddress : Int, nodeType : String, priority : Int) {
    open fun getAssociations() : List<String> {
        return listOf()
    }
}