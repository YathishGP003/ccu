package a75f.io.domain.model.common

/**
 * Helper to shorten validations with standard error messaging
 */
class Validation {
    companion object {
        fun isNotBlank(strField: String, fieldName: String) {
            require(strField.isNotBlank()) {
                "$fieldName must not be blank"
            }
        }

        fun isNotBlank(stringsToNames: List<Pair<String, String>>) {
            stringsToNames.forEach {
                isNotBlank(it.first, it.second)
            }
        }
    }
}
