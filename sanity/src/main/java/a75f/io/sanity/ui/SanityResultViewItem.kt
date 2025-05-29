package a75f.io.sanity.ui

import a75f.io.sanity.framework.SanityResultType

data class SanityResultViewItem(
    val testName: String,
    val description: String,
    val result: SanityResultType
)
