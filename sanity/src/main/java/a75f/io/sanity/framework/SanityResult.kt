package a75f.io.sanity.framework

data class SanityResult(
    val name: String,
    val result: SanityResultType,
    var report: String,
    var corrected: Boolean = false
)

enum class SanityResultType {
    PASSED,
    FAILED,
    PENDING
}