package a75f.io.sanity.framework

interface SanityCase {
    fun getName(): String
    fun execute(): Boolean
    fun report(): String

    /**
     * If the intention of the SanityCase is only to report , correct() method should return false
     */
    fun correct(): Boolean

    fun getDescription(): String {
        return "No description provided for ${getName()}"
    }
    fun getSeverity(): Int {
        return 0 // Default severity
    }
}