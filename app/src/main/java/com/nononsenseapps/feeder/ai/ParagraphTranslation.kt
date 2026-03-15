package com.nononsenseapps.feeder.ai

import kotlinx.serialization.Serializable

@Serializable
data class ParagraphTranslation(
    val index: Int,
    val text: String,
    val translation: String,
    val translated: Int,
)
