package com.nononsenseapps.feeder.ai

import kotlinx.serialization.Serializable

@Serializable
data class ArticleTranslation(
    val contents: List<ParagraphTranslation>,
    val status: String,
) {
    val paragraphCompletedCount: Int
        get() = contents.count { it.translated == 1 }

    val paragraphFailedCount: Int
        get() = contents.count { it.translated == -1 }

    val paragraphTotalCount: Int
        get() = contents.size

    val isAllCompleted: Boolean
        get() = contents.all { it.translated != 0 }

    fun buildTranslatedParagraphsList(): List<String?> =
        contents.map { paragraph ->
            if (paragraph.translated == 1) paragraph.translation else null
        }
}
