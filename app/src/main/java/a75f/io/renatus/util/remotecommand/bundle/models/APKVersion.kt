package a75f.io.renatus.util.remotecommand.bundle.models

class APKVersion(version: String? = null) : Comparable<APKVersion> {
    companion object {
        private var regex = Regex("[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}")
        fun factory(version: String? = null) : APKVersion? {
            try {
                if (version == null || version.isNullOrBlank()) {
                    return null
                }
                return APKVersion(version)
            } catch(e: Exception) {
                return null
            }
        }

        fun extractVersion(version: String?): String? { // CCU_qa_2.17.3.apk
            if (version == null) {
                return null
            }
            val match = regex.find(version)
            return match?.value
        }

        fun compareVersions(a: String, b: String): Int {
            val versionA = factory(a)
            val versionB = factory(b)
            if (versionA == null || versionB == null) {
                return 0
            }
            return versionA.compareTo(versionB)
        }
    }

    private val components: List<Int>?
    private val major: Int
    private val minor: Int
    private val patch: Int

    init {
        components = extractVersion(version)?.split(".")?.map{ it.toInt() }
        if (components != null) {
            major = components[0]
            minor = components[1]
            patch = components[2]
        } else {
            major = 0
            minor = 0
            patch = 0
        }
    }

    override fun compareTo(other: APKVersion): Int {
        if (major != other.major) {
            return major.compareTo(other.major)
        }
        if (minor != other.minor) {
            return minor.compareTo(other.minor)
        }
        return patch.compareTo(other.patch)
    }

    override fun equals(other: Any?): Boolean {
        if (other is APKVersion) {
            if (major != other.major) {
                return major.compareTo(other.major) == 0
            }
            if (minor != other.minor) {
                return minor.compareTo(other.minor) == 0
            }
            return patch.compareTo(other.patch) == 0
        }
        return false
    }
}
