package a75f.io.sanity.framework

class SanitySuite( val name: String) {
    private val sanityCases = mutableListOf<SanityCase>()

    fun addCase(case: SanityCase) {
        sanityCases.add(case)
    }

    fun removeCase(case: SanityCase) {
        sanityCases.remove(case)
    }

    fun getCases(): List<SanityCase> {
        return sanityCases.toList()
    }
}