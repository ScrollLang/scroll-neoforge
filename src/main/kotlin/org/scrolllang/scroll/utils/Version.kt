package org.scrolllang.scroll.utils

import java.io.Serializable
import java.util.regex.Pattern

data class Version(
    private val version: Array<Int?> = arrayOfNulls(3),
    private var postfix: String? = null
) : Serializable, Comparable<Version> {

    companion object {
        private val versionPattern = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:([-+])(.*))?")
        fun compare(v1: String, v2: String): Int = Version(v1).compareTo(Version(v2))
    }

    constructor(vararg version: Int) : this(
        version = Array(3) { version.getOrNull(it) },
        postfix = null
    ) {
        require(version.size in 1..3) { "Versions must have 2 or 3 numbers (${version.size} given)" }
    }

    constructor(major: Int, minor: Int, postfix: String?) : this(
        version = arrayOf(major, minor, null),
        postfix = postfix?.takeIf { it.isNotEmpty() }
    )

    constructor(version: String) : this() {
        val matcher = versionPattern.matcher(version.trim())
        require(matcher.matches()) { "'$version' is not a valid version string" }
        for (i in 0..2) this.version[i] = matcher.group(i + 1)?.toIntOrNull()
        this.postfix = matcher.group(4)
    }

    override fun compareTo(other: Version): Int {
        for (i in version.indices) {
            val diff = (version[i] ?: 0).compareTo(other.version[i] ?: 0)
            if (diff != 0) return diff
        }
        return postfix?.compareTo(other.postfix ?: "") ?: -1
    }

    fun isSmallerThan(other: Version): Boolean = compareTo(other) < 0
    fun isLargerThan(other: Version): Boolean = compareTo(other) > 0
    fun isStable(): Boolean = postfix == null
    fun getMajor(): Int = version[0] ?: 0
    fun getMinor(): Int = version[1] ?: 0
    fun getRevision(): Int = version[2] ?: 0

    override fun toString(): String {
        val revision = version[2]?.let { ".$it" } ?: ""
        val postfixString = postfix?.let { "-$it" } ?: ""
        return "${version[0]}.${version[1]}$revision$postfixString"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Version

        if (!version.contentEquals(other.version)) return false
        if (postfix != other.postfix) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version.contentHashCode()
        result = 31 * result + (postfix?.hashCode() ?: 0)
        return result
    }

}
