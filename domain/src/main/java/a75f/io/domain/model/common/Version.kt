package a75f.io.domain.model.common

class Version(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<Version> {
    override fun toString(): String {
        return "v$major.$minor.$patch"
    }

    override fun compareTo(other: Version): Int {
        (listOf(major, minor, patch) zip listOf(other.major, other.minor, other.patch)).forEach { (a, b) ->
            if (a > b) return 1
            else if (a < b) return -1
        }
        return 0
    }

    override fun equals(other: Any?): Boolean {
        return (other as? Version) ?.let { major == other.major && minor == other.minor && patch == other.patch } ?: false
    }

    companion object {
        fun parse(version: String): Version {
            val components = version.split(VERSION_DELIMITER)
            require(components.mapNotNull { it.toIntOrNull() }.let { it.size == 3 && it.all { value -> value >= 0 } }) {
                "Version \"$version\" must be of format \"X.Y.Z\" where X, Y, Z are positive integers"
            }

            return Version(components[0].toInt(), components[1].toInt(), components[2].toInt())
        }

        private const val VERSION_DELIMITER = "."
    }
}
