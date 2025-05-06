package a75f.io.logic.preconfig

sealed class PreconfigurationState {
    object NotConfigured : PreconfigurationState()
    object Started : PreconfigurationState()
    object Downloaded : PreconfigurationState()
    object Progress : PreconfigurationState()
    object Completed : PreconfigurationState()
    object Failed : PreconfigurationState()

    companion object {
        fun fromString(value: String): PreconfigurationState {
            return when (value) {
                "NotConfigured" -> NotConfigured
                "Started" -> Started
                "Downloaded" -> Downloaded
                "Progress" -> Progress
                "Completed" -> Completed
                "failed" -> Failed
                else -> NotConfigured
            }
        }
    }
    fun toStringValue(): String {
        return when (this) {
            is NotConfigured -> "NotConfigured"
            is Started -> "Started"
            is Downloaded -> "Downloaded"
            is Progress -> "Progress"
            is Completed -> "Completed"
            is Failed -> "Failed"
        }
    }
}