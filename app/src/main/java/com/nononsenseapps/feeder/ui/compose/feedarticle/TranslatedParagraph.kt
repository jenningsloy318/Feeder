package com.nononsenseapps.feeder.ui.compose.feedarticle

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Composable that displays a paragraph with its optional translation.
 *
 * The original paragraph is displayed normally, and if a translation is provided,
 * it is shown below in a secondary container style with subtle alpha.
 *
 * @param originalParagraph The original paragraph text
 * @param translatedParagraph The translated paragraph text, or null if no translation exists
 * @param modifier Modifier for the composable
 */
@Composable
fun TranslatedParagraph(
    originalParagraph: String,
    translatedParagraph: String?,
    modifier: Modifier = Modifier,
) {
    // Display original paragraph
    Text(
        text = originalParagraph,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier.fillMaxWidth(),
    )

    // Display translated paragraph if available
    if (translatedParagraph != null) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = LocalTextStyle.current.color.copy(alpha = 0.7f),
                    ),
                ) {
                    append(translatedParagraph)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
        )
    }
}
