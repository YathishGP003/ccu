package a75f.io.sanity.framework

data class SanityResult(
    val name: String,
    val result: SanityResultType,
    var report: String,
    var corrected: Boolean = false,
    var severity : SanityResultSeverity
)

enum class SanityResultType {
    PASSED,
    FAILED,
    PENDING
}

/**
 * Define how the result of a sanity case would impact a Site.
 * Failure of a HIGH Severity case would mean critical issues in the site and generates an alert.
 */
enum class SanityResultSeverity {
    LOW,
    MEDIUM,
    HIGH
}